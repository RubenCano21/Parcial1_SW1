package org.example.schemaflow.shared.presentation.controller;

import lombok.RequiredArgsConstructor;
import org.example.schemaflow.shared.application.service.AIAssistantService;
import org.example.schemaflow.shared.domain.dto.gemini.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai-assistant")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AIAssistantController {

    private final AIAssistantService aiService;

    @PostMapping("/analyze-diagram")
    public ResponseEntity<AIRelationshipSuggestion> analyzeDiagram(
            @RequestBody DiagramContext context) {
        try {
            AIRelationshipSuggestion result = aiService.analyzeDiagram(context);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/chat")
    public ResponseEntity<AIChatResponse> chat(@RequestBody ChatRequest request) {
        try {
            AIChatResponse response = aiService.chat(
                    request.getMessage(),
                    request.getHistory()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/validate-relationship")
    public ResponseEntity<ValidationResponse> validateRelationship(
            @RequestBody RelationshipValidationRequest request) {
        try {
            ValidationResponse response = aiService.validateRelationship(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
