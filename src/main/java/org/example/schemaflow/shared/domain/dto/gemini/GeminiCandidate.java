package org.example.schemaflow.shared.domain.dto.gemini;

import lombok.Data;

@Data
public class GeminiCandidate {

    private GeminiContent content;
    private String finishReason;
}
