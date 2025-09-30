package org.example.schemaflow.shared.domain.dto.gemini;

import lombok.Data;

@Data
public class GeminiResponse {

    private GeminiCandidate[] candidates;
}
