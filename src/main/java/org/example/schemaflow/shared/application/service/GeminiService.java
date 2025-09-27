package org.example.schemaflow.shared.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    @Value("${api.gemini.url}")
    private String apiUrl;

    @Value("${api.gemini.key}")
    private String apiKey;

    private final ERDParserService erdParserService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, String> generateCode(String erdJson) {
        try {
            log.info("Generating code from ERD JSON using Gemini API");


            // PASO 1: Parsear el JSON del diagrama ER a un prompt estructurado
            String structuredPrompt = erdParserService.parseERDToStructuredPrompt(erdJson);
            log.debug("Structured Prompt created, length {}", structuredPrompt.length());

            // PASO 2: Construir el prompt completo para Gemini
            String fullPrompt = buildGeminiPrompt(structuredPrompt);

            // PASO 3: Llamar a la API de Gemini
            Map<String, String> generatedFiles = callGeminiAPI(fullPrompt);

            // PASO 4: Procesar y limpiar los archivos generados
            Map<String, String> cleanedFiles = cleanAndProcessFiles(generatedFiles);

            log.info("Successfully generated {} files", cleanedFiles.size());
            cleanedFiles.keySet().forEach(filename -> log.debug("Generated file: {}", filename));
            return cleanedFiles;

        } catch (Exception e) {
            log.error("Error generating code with Gemini", e);
            throw new RuntimeException("Failed to generate code with Gemini: " + e.getMessage(), e);
        }
    }

    private String buildGeminiPrompt(String structuredData) {
        return String.format("""
                Eres un experto desarrollador Spring Boot. Basándote en la siguiente información estructurada de un diagrama ER, 
                genera el código Java completo para un proyecto Spring Boot 3.2+ con Java 17.
                
                INFORMACIÓN DEL DIAGRAMA:
                %s
                
                REGLAS DE GENERACIÓN:
                1. Paquete base: com.example.project
                2. Usar Spring Boot 3.2+ con Jakarta EE
                3. Implementar todas las capas: Entity, DTO, Repository, Service, Controller, Mapper
                4. Usar Lombok para reducir boilerplate (@Data, @NoArgsConstructor, @AllArgsConstructor, @Builder)
                5. Usar MapStruct para mappers (@Mapper(componentModel = "spring"))
                6. Implementar relaciones JPA correctamente
                7. Usar validaciones Jakarta Bean Validation (@NotNull, @Size, @Valid)
                8. Crear DTOs sin referencias circulares
                9. Implementar operaciones CRUD completas
                10. Usar ResponseEntity en controllers
                11. Manejar errores apropiadamente
                
                ESTRUCTURA DE ARCHIVOS A GENERAR:
                Para cada entidad generar:
                - [Entity].java en package com.example.project.entity
                - [Entity]Dto.java en package com.example.project.dto  
                - [Entity]Repository.java en package com.example.project.repository
                - [Entity]Service.java en package com.example.project.service
                - [Entity]ServiceImpl.java en package com.example.project.service.impl
                - [Entity]Controller.java en package com.example.project.controller
                - [Entity]Mapper.java en package com.example.project.mapper
                
                FORMATO DE RESPUESTA REQUERIDO:
                Devuelve ÚNICAMENTE un JSON válido sin markdown con esta estructura:
                {
                  "Alumno.java": "package com.example.project.entity;\\n\\nimport jakarta.persistence.*;\\nimport lombok.Data;\\n\\n@Entity\\n@Table(name = \\"alumnos\\")\\n@Data\\npublic class Alumno {\\n    @Id\\n    @GeneratedValue(strategy = GenerationType.IDENTITY)\\n    private Long id;\\n    private String nombre;\\n}",
                  "AlumnoDto.java": "package com.example.project.dto;\\n\\nimport lombok.Data;\\n\\n@Data\\npublic class AlumnoDto {\\n    private Long id;\\n    private String nombre;\\n}"
                }
                
                IMPORTANTE:
                - Las claves deben ser SOLO el nombre del archivo (ejemplo: "Alumno.java", NO rutas)
                - Usar \\n para saltos de línea en el contenido
                - No incluir explicaciones, solo el JSON
                - No usar bloques de código markdown
                - Asegurar que todos los imports sean correctos
                - Implementar las relaciones JPA según las cardinalidades especificadas
                """, structuredData);
    }

    private Map<String, String> callGeminiAPI(String prompt) throws Exception {
        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", prompt)
                                )
                        )
                ),
                "generationConfig", Map.of(
                        "temperature", 0.1,
                        "maxOutputTokens", 8192,
                        "candidateCount", 1
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        String fullUrl = apiUrl + "?key=" + apiKey;
        log.debug("Calling Gemini API: {}", fullUrl);

        ResponseEntity<String> response = restTemplate.exchange(
                fullUrl,
                HttpMethod.POST,
                request,
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Gemini API returned error: " + response.getStatusCode());
        }

        // Parsear la respuesta
        JsonNode responseJson = objectMapper.readTree(response.getBody());
        JsonNode candidatesNode = responseJson.get("candidates");

        if (candidatesNode != null && !candidatesNode.isEmpty()) {
            JsonNode contentNode = candidatesNode.get(0).get("content");
            if (contentNode != null) {
                JsonNode partsNode = contentNode.get("parts");
                if (partsNode != null && !partsNode.isEmpty()) {
                    String generatedText = partsNode.get(0).get("text").asText();
                    log.debug("Raw Gemini response length: {}", generatedText.length());

                    // Extraer y parsear el JSON
                    String jsonContent = extractJsonFromResponse(generatedText);
                    return objectMapper.readValue(jsonContent, new TypeReference<Map<String, String>>() {});
                }
            }
        }

        throw new RuntimeException("No valid response received from Gemini API");
    }

    private String extractJsonFromResponse(String response) {
        String cleaned = response.trim();
        log.debug("Extracting JSON from response (first 200 chars): {}",
                cleaned.substring(0, Math.min(200, cleaned.length())));

        // Remover bloques de código markdown si existen
        cleaned = cleaned.replaceAll("```json\\s*", "").replaceAll("```\\s*$", "");

        // Buscar el JSON entre llaves
        int startIndex = cleaned.indexOf('{');
        int endIndex = cleaned.lastIndexOf('}');

        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            String jsonContent = cleaned.substring(startIndex, endIndex + 1);
            log.debug("Extracted JSON boundaries: start={}, end={}", startIndex, endIndex);
            return jsonContent;
        }

        log.warn("No clear JSON boundaries found, returning whole response");
        return cleaned;
    }

    private Map<String, String> cleanAndProcessFiles(Map<String, String> originalFiles) {
        Map<String, String> processedFiles = new HashMap<>();

        for (Map.Entry<String, String> entry : originalFiles.entrySet()) {
            String originalKey = entry.getKey();
            String content = entry.getValue();

            // Limpiar la clave: extraer solo el nombre del archivo
            String cleanKey = extractFileName(originalKey);

            // Procesar el contenido: convertir \\n a saltos de línea reales
            String cleanContent = unescapeContent(content);

            log.debug("Processing file: '{}' -> '{}' (content length: {})",
                    originalKey, cleanKey, cleanContent.length());

            processedFiles.put(cleanKey, cleanContent);
        }

        return processedFiles;
    }

    private String extractFileName(String key) {
        // Si la clave contiene rutas, extraer solo el nombre del archivo
        if (key.contains("/")) {
            String[] parts = key.split("/");
            return parts[parts.length - 1];
        }

        if (key.contains("\\")) {
            String[] parts = key.split("\\\\");
            return parts[parts.length - 1];
        }

        return key.trim();
    }

    private String unescapeContent(String content) {
        if (content == null) return "";

        return content
                .replace("\\n", "\n")           // Saltos de línea
                .replace("\\t", "\t")           // Tabs
                .replace("\\r", "\r")           // Retorno de carro
                .replace("\\\"", "\"")          // Comillas dobles
                .replace("\\'", "'")            // Comillas simples
                .replace("\\\\", "\\");         // Backslashes (debe ir al final)
    }
}