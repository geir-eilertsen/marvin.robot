package com.assetvisor.marvin.brain.springai.adapters;

import com.assetvisor.marvin.robot.domain.brain.AsleepException;
import com.assetvisor.marvin.robot.domain.brain.Brain;
import com.assetvisor.marvin.robot.domain.brain.ForInvokingIntelligence;
import com.assetvisor.marvin.robot.domain.communication.ConversationMessage;
import com.assetvisor.marvin.robot.domain.communication.Message;
import com.assetvisor.marvin.robot.domain.communication.SpeechMessage;
import com.assetvisor.marvin.robot.domain.communication.TextMessage;
import com.assetvisor.marvin.robot.domain.environment.EnvironmentDescription;
import com.assetvisor.marvin.robot.domain.environment.Observation;
import com.assetvisor.marvin.robot.domain.jobdescription.RobotDescription;
import com.assetvisor.marvin.robot.domain.tools.Tool;
import com.assetvisor.marvin.toolkit.memory.ForRemembering;
import jakarta.annotation.Resource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.client.ChatClient.PromptUserSpec;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.cassandra.CassandraVectorStore;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static com.assetvisor.marvin.robot.domain.communication.ConversationMessage.DEFAULT_CONVERSATION_ID;

@Component
@Profile("chat-openai")
public class BrainSpringAiAdapterOpenAi implements ForInvokingIntelligence, ForRemembering {

    private final Log LOG = LogFactory.getLog(BrainSpringAiAdapterOpenAi.class);
    @SuppressWarnings("FieldCanBeLocal")
    private final int VECTORSTORE_TOP_K = 20;
    private final int CHAT_MEMORY_RETRIEVE_SIZE = 10;
    private final ChatModel CHAT_MODEL_TEXT = ChatModel.GPT_4_O_MINI;
    private final ChatModel CHAT_MODEL_AUDIO = ChatModel.GPT_4_O_AUDIO_PREVIEW;
    private final ChatModel CHAT_MODEL_OBSERVATION = ChatModel.GPT_4_O_MINI;

    @Resource
    private CassandraVectorStore vectorStore;
    @Resource
    private ChatMemory chatMemory;
    @Resource
    private ChatClient.Builder chatClientBuilder;

    private ChatClient chatClient;

    @Override
    public void teach(
        List<EnvironmentDescription> environmentDescriptions
    ) {
        this.vectorStore.add(
            environmentDescriptions.stream()
                .map(this::map)
                .collect(Collectors.toList())
        );
    }

    @Override
    public void wakeUp(
        RobotDescription robotDescription,
        List<Tool<?, ?>> environmentFunctions
    ) {
        LOG.info("Waking up Brain...");

        this.chatClient = chatClientBuilder
            .defaultSystem(robotDescription.text())
            .defaultAdvisors(
                MessageChatMemoryAdvisor
                    .builder(chatMemory)
                    .build(),
                QuestionAnswerAdvisor
                    .builder(vectorStore)
                    .searchRequest(SearchRequest
                        .builder()
                        .topK(VECTORSTORE_TOP_K)
                        .build()
                    )
                    .build(),
                new SimpleLoggerAdvisor()
            )
            .defaultToolCallbacks(environmentFunctions
                .stream()
                .map(this::map)
                .toArray(ToolCallback[]::new)
            )
            .build();
        LOG.info("Brain woken up.");
    }

    private ToolCallback map(Tool<?, ?> environmentFunction) {
        return FunctionToolCallback.builder(environmentFunction.name(), environmentFunction)
            .inputType(environmentFunction.inputType())
            .description(environmentFunction.description())
            .build();
    }

    private Document map(EnvironmentDescription environmentDescription) {
        return new Document(environmentDescription.text());
    }

    @Override
    public void invoke(ConversationMessage message, Brain brain) {
        doInvoke(message, brain, DEFAULT_CONVERSATION_ID);
    }

    @Override
    public void invoke(Observation observation, Brain brain) {
        doInvoke(observation, brain, DEFAULT_CONVERSATION_ID);
    }

    private void doInvoke(Message message, Brain brain, String conversationId) {
        if (chatClient == null) {
            throw new AsleepException("Brain is asleep, please wake it up first.");
        }

        ChatClientRequestSpec chatClientRequestSpec = chatClient.prompt()
            .user(u -> promptUserSpec(u, message))
            .options(options(message))
            .advisors(a -> a
                .param(ChatMemory.CONVERSATION_ID, conversationId)
                // TODO: Probably superseded by MessageWindowChatMemory.maxMessages, which is 20 by default
                //.param(AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY, CHAT_MEMORY_RETRIEVE_SIZE)
            );

        ChatResponse chatResponse = chatClientRequestSpec
            .call().chatResponse();

        assert chatResponse != null;
        String responseString = chatResponse.getResult().getOutput().getText();
        brain.respond(responseString, conversationId);
    }

    private void promptUserSpec(PromptUserSpec promptUserSpec, Message message) {
        if(message instanceof TextMessage textMessage) {
            promptUserSpec.text(textMessage.getContent());
        }
        if(message instanceof Observation observation) {
            promptUserSpec.text("Observation from the environment: " + observation);
        }
        if(message instanceof SpeechMessage speechMessage) {
            promptUserSpec.text("Please respond to the audio message.");
            promptUserSpec.media(
                MediaType.parseMediaType("audio/wav"),
                new ByteArrayResource(speechMessage.getAudio())
            );
        }
    }

    private OpenAiChatOptions options(Message message) {
        if(message instanceof TextMessage textMessage) {
            return OpenAiChatOptions.builder()
                //.model(CHAT_MODEL_TEXT)
                .build();
        }
        if(message instanceof SpeechMessage speechMessage) {
            return OpenAiChatOptions.builder()
                .model(CHAT_MODEL_AUDIO)
                .build();
        }
        if(message instanceof Observation observation) {
            return OpenAiChatOptions.builder()
                //.model(CHAT_MODEL_OBSERVATION)
                .build();
        }
        throw new IllegalArgumentException("Unknown message type: " + message.getClass());
    }

    @Override
    public void remember(String thought) {
        vectorStore.add(List.of(new Document(thought)));
    }
}
