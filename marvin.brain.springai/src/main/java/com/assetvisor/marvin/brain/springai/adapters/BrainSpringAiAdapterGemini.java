package com.assetvisor.marvin.brain.springai.adapters;

import static com.assetvisor.marvin.robot.domain.communication.ConversationMessage.DEFAULT_CONVERSATION_ID;

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
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.client.ChatClient.PromptUserSpec;
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallback.SchemaType;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.cassandra.CassandraVectorStore;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel.ChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Profile("gemini")
@Component
public class BrainSpringAiAdapterGemini implements ForInvokingIntelligence, ForRemembering {

    private final Log LOG = LogFactory.getLog(BrainSpringAiAdapterGemini.class);
    @SuppressWarnings("FieldCanBeLocal")
    private final int VECTORSTORE_TOP_K = 20;
    private final int CHAT_MEMORY_RETRIEVE_SIZE = 10;

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
                new MessageChatMemoryAdvisor(chatMemory),
                new QuestionAnswerAdvisor(
                    vectorStore,
                    SearchRequest
                        .builder()
                        .topK(VECTORSTORE_TOP_K)
                        .build()
                ),
                new SimpleLoggerAdvisor()
            )
            .defaultFunctions(environmentFunctions
                .stream()
                .map(this::map)
                .toArray(FunctionCallback[]::new)
            )
            .build();
        LOG.info("Brain woken up.");
    }

    private FunctionCallback map(Tool<?, ?> environmentFunction) {
        return FunctionCallback.builder()
            .function(environmentFunction.name(), environmentFunction)
            .description(environmentFunction.description())
            .schemaType(SchemaType.OPEN_API_SCHEMA)
            .inputType(environmentFunction.inputType())
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
                .param(AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId)
                .param(AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY, CHAT_MEMORY_RETRIEVE_SIZE)
            );

        ChatResponse chatResponse = chatClientRequestSpec
            .call().chatResponse();

        assert chatResponse != null;
        String responseString = chatResponse.getResult().getOutput().getContent();
        brain.respond(responseString, conversationId);
    }

    private void promptUserSpec(PromptUserSpec promptUserSpec, Message message) {
        if(message instanceof TextMessage textMessage) {
            promptUserSpec.text(textMessage.getSender() + " says " + textMessage.getContent());
        }
        if(message instanceof Observation observation) {
            promptUserSpec.text("Observation of the environment: " + observation);
        }
        if(message instanceof SpeechMessage speechMessage) {
            promptUserSpec.text("Please respond to the audio message.");
            promptUserSpec.media(
                MediaType.parseMediaType("audio/wav"),
                new ByteArrayResource(speechMessage.getAudio())
            );
        }
    }

    private VertexAiGeminiChatOptions options(Message message) {
        if(message instanceof TextMessage textMessage) {
            return VertexAiGeminiChatOptions.builder()
                .model(ChatModel.GEMINI_1_5_FLASH)
                .build();
        }
        if(message instanceof SpeechMessage speechMessage) {
            return VertexAiGeminiChatOptions.builder()
                .model(ChatModel.GEMINI_1_5_FLASH)
                .build();
        }
        if(message instanceof Observation observation) {
            return VertexAiGeminiChatOptions.builder()
                .model(ChatModel.GEMINI_1_5_FLASH)
                .build();
        }
        throw new IllegalArgumentException("Unknown message type: " + message.getClass());
    }

    @Override
    public void remember(String thought) {
        vectorStore.add(List.of(new Document(thought)));
    }
}
