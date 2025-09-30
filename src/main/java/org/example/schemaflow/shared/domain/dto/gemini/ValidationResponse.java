package org.example.schemaflow.shared.domain.dto.gemini;

import lombok.Data;

import java.util.List;

@Data
public class ValidationResponse {
    private Boolean isValid;
    private Boolean cardinalityCorrect;
    private List<String> suggestions;
    private Double confidence;
    private String explanation;
}
