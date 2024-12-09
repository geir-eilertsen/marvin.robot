package com.assetvisor.marvin.interaction.web.controllers;

import com.assetvisor.marvin.interaction.web.AudioBuffer;
import com.assetvisor.marvin.robot.application.ListenUseCase;
import jakarta.annotation.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HumanInteractionsController {

    @Resource
    private ListenUseCase listenUseCase;
    @Resource
    private AudioBuffer audioBuffer;

    @PostMapping("/message")
    public void message(@RequestParam(value = "message") String message) {
        listenUseCase.listenTo(message);
    }

    @PostMapping(path = "/speech", consumes = "application/octet-stream")
    public void speech(@RequestBody byte[] message) {
        listenUseCase.listenTo(message);
    }

    @GetMapping("/speech")
    public ResponseEntity<byte[]> speech() {
        byte[] audioData = audioBuffer.get();

        if (audioData == null || audioData.length == 0) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=chat.mp3");
        headers.add(HttpHeaders.CONTENT_TYPE, "audio/mpeg");

        return new ResponseEntity<>(audioData, headers, HttpStatus.OK);
    }
}
