package org.example.schemaflow.shared.domain.dto.gemini;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GeminiContent {

    private  GeminiPart[] parts;
    private String role;
}
