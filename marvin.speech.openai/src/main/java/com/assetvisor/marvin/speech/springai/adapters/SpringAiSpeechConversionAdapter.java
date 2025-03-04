package com.assetvisor.marvin.speech.springai.adapters;

import com.assetvisor.marvin.robot.domain.communication.ForConvertingSpeechToText;
import com.assetvisor.marvin.robot.domain.communication.ForConvertingTextToSpeech;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.util.logging.Logger;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi.SpeechRequest.AudioResponseFormat;
import org.springframework.ai.openai.api.OpenAiAudioApi.SpeechRequest.Voice;
import org.springframework.ai.openai.api.OpenAiAudioApi.TranscriptResponseFormat;
import org.springframework.ai.openai.api.OpenAiAudioApi.TtsModel;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.ai.openai.audio.speech.SpeechResponse;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class SpringAiSpeechConversionAdapter implements ForConvertingTextToSpeech, ForConvertingSpeechToText {

    private final Logger LOG = Logger.getLogger(getClass().getName());

    @Resource
    private Environment environment;
    @Resource
    private OpenAiAudioSpeechModel openAiAudioSpeechModel;
    @Resource
    private OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel;

    @PostConstruct
    public void init() {
        LOG.info("Language: " + environment.getProperty("marvin.language","en"));
    }

    @Override
    public byte[] convert(String text) {
        return new byte[0];
//        OpenAiAudioSpeechOptions speechOptions = OpenAiAudioSpeechOptions.builder()
//            .responseFormat(AudioResponseFormat.MP3)
//            .speed(1.0f)
//            .model(TtsModel.TTS_1.value)
//            .voice(Voice.FABLE)
//            .build();
//
//        var speechPrompt = new SpeechPrompt(text, speechOptions);
//        SpeechResponse response = openAiAudioSpeechModel.call(speechPrompt);
//        return response.getResult().getOutput();
    }

    @Override
    public String convert(byte[] speech) {
        return "";
//        OpenAiAudioTranscriptionOptions transcriptionOptions = OpenAiAudioTranscriptionOptions.builder()
//            .responseFormat(TranscriptResponseFormat.TEXT)
//            .temperature(0.0f)
//            .language(environment.getProperty("marvin.language","en"))
//            .build();
//
//        org.springframework.core.io.Resource audioResource = new org.springframework.core.io.ByteArrayResource(speech);
//
//        var audioTranscriptionPrompt = new AudioTranscriptionPrompt(audioResource, transcriptionOptions);
//        var response = openAiAudioTranscriptionModel.call(audioTranscriptionPrompt);
//        return response.getResult().getOutput();
    }
}
