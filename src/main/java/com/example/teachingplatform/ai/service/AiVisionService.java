package com.example.teachingplatform.ai.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

@Service
public class AiVisionService {

    private final BigModelChatService bigModelChatService;

    public AiVisionService(BigModelChatService bigModelChatService) {
        this.bigModelChatService = bigModelChatService;
    }

    public String analyzeImage(String prompt, MultipartFile image, String fallbackReply) {
        if (image == null || image.isEmpty()) {
            return fallbackReply;
        }
        try {
            String base64 = Base64.getEncoder().encodeToString(image.getBytes());
            return bigModelChatService.visionChat(prompt, base64, fallbackReply);
        } catch (IOException e) {
            return fallbackReply;
        }
    }
}
