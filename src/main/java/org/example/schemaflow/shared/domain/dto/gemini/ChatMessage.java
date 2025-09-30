package org.example.schemaflow.shared.domain.dto.gemini;

import lombok.Data;

@Data
public class ChatMessage {

    private String role;
    private String content;
}
