package org.example.schemaflow.shared.domain.dto.gemini;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GeminiGenerationConfig {

    private Double temperature;
    private Integer topK;
    private Double topP;
    private Integer maxOutputTokens;
}
