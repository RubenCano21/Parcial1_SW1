package org.example.schemaflow.shared.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.schemaflow.shared.domain.dto.gemini.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIAssistantService {

    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    /**
     * Analiza el diagrama y sugiere relaciones
     */
    public AIRelationshipSuggestion analyzeDiagram(DiagramContext context) {
        try {
            String prompt = buildDiagramAnalysisPrompt(context);
            String response = geminiService.generateContent(prompt);

            // Extraer JSON de la respuesta (Gemini puede envolver en markdown)
            String jsonResponse = extractJSON(response);

            return objectMapper.readValue(jsonResponse, AIRelationshipSuggestion.class);
        } catch (Exception e) {
            log.error("Error analyzing diagram", e);
            throw new RuntimeException("Error al analizar el diagrama", e);
        }
    }

    /**
     * Chatbot para ayuda al usuario
     */
    public AIChatResponse chat(String userMessage, List<ChatMessage> history) {
        String systemPrompt = """
            Eres un asistente experto en modelado de bases de datos y diagramas Entidad-Relación (ER).
            
            Tu función es ayudar a usuarios a:
            - Crear diagramas ER correctos y bien estructurados
            - Entender conceptos como cardinalidad (1:1, 1:N, N:M)
            - Comprender tipos de relaciones
            - Aplicar principios de normalización
            - Diseñar esquemas de bases de datos eficientes
            
            Siempre sé claro, didáctico y proporciona ejemplos cuando sea apropiado.
            Responde en español de forma concisa pero completa.
            """;

        String response = geminiService.generateContentWithHistory(
                systemPrompt, userMessage, history
        );

        return AIChatResponse.builder()
                .response(response)
                .timestamp(java.time.LocalDateTime.now())
                .build();
    }

    /**
     * Valida una relación propuesta
     */
    public ValidationResponse validateRelationship(RelationshipValidationRequest request) {
        String prompt = String.format("""
            Evalúa si la siguiente relación en un diagrama ER es correcta:
            
            Entidad Origen: %s (Atributos: %s)
            Entidad Destino: %s (Atributos: %s)
            Cardinalidad: %s
            Tipo de Relación: %s
            
            Por favor indica:
            1. ¿Es válida esta relación? (true/false)
            2. ¿La cardinalidad es apropiada?
            3. Sugerencias de mejora si las hay
            4. Nivel de confianza (0-1)
            
            Responde SOLO en formato JSON:
            {
                "isValid": boolean,
                "cardinalityCorrect": boolean,
                "suggestions": ["sugerencia1", "sugerencia2"],
                "confidence": number,
                "explanation": "string"
            }
            """,
                request.getFromEntity(),
                String.join(", ", request.getFromAttributes()),
                request.getToEntity(),
                String.join(", ", request.getToAttributes()),
                request.getCardinality(),
                request.getRelationType()
        );

        try {
            String response = geminiService.generateContent(prompt);
            String jsonResponse = extractJSON(response);
            return objectMapper.readValue(jsonResponse, ValidationResponse.class);
        } catch (Exception e) {
            log.error("Error validating relationship", e);
            throw new RuntimeException("Error al validar la relación", e);
        }
    }

    /**
     * Construye el prompt para análisis de diagrama
     */
    private String buildDiagramAnalysisPrompt(DiagramContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analiza el siguiente diagrama Entidad-Relación y sugiere relaciones faltantes:\n\n");

        prompt.append("ENTIDADES:\n");
        for (Entity entity : context.getEntities()) {
            prompt.append(String.format("- %s: %s\n",
                    entity.getName(),
                    String.join(", ", entity.getAttributes())
            ));
        }

        prompt.append("\nRELACIONES EXISTENTES:\n");
        if (context.getExistingRelationships().isEmpty()) {
            prompt.append("- Ninguna\n");
        } else {
            for (Relationship rel : context.getExistingRelationships()) {
                prompt.append(String.format("- %s %s %s\n",
                        rel.getFromEntity(),
                        rel.getCardinality(),
                        rel.getToEntity()
                ));
            }
        }

        prompt.append("""
            
            Por favor sugiere relaciones faltantes que sean lógicas y apropiadas.
            Para cada sugerencia, indica:
            - Entidad origen
            - Entidad destino
            - Cardinalidad recomendada (1:1, 1:N, o N:M)
            - Razón de la sugerencia
            - Nivel de confianza (0.0 a 1.0)
            
            Responde SOLO en formato JSON (sin markdown):
            {
                "suggestions": [
                    {
                        "fromEntity": "string",
                        "toEntity": "string",
                        "cardinality": "string",
                        "reason": "string",
                        "confidence": number
                    }
                ],
                "explanation": "string"
            }
            """);

        return prompt.toString();
    }

    /**
     * Extrae JSON de una respuesta que puede estar envuelta en markdown
     */
    private String extractJSON(String response) {
        // Gemini a veces envuelve JSON en ```json ... ```
        if (response.contains("```json")) {
            int start = response.indexOf("```json") + 7;
            int end = response.lastIndexOf("```");
            return response.substring(start, end).trim();
        } else if (response.contains("```")) {
            int start = response.indexOf("```") + 3;
            int end = response.lastIndexOf("```");
            return response.substring(start, end).trim();
        }
        return response.trim();
    }
}
