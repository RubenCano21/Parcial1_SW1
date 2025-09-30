package org.example.schemaflow.shared.domain.dto.gemini;

import lombok.Data;

import java.util.List;

@Data
public class ChatRequest {
    private String message;
    private List<ChatMessage> history;
}
