package org.example.schemaflow.shared.domain.dto.gemini;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AIRelationshipSuggestion {
    private List<SuggestedRelationship> suggestions;
    private String explanation;
}
