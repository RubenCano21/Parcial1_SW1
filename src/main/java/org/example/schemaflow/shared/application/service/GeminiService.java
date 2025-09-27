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

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, String> generateCode(String erdJson) {
        try {
            log.info("Generating code from ERD JSON using Gemini API");

            String promptText = """
                    Eres un experto generador de código Spring Boot. A partir del siguiente JSON de un diagrama ER, genera ÚNICAMENTE el código Java.
                    
                    ERD JSON:
                    %s
                    
                    INSTRUCCIONES IMPORTANTES:
                    1. Genera código Spring Boot 3.2+ con Java 17
                    2. Usa estas dependencias: JPA, Lombok, MapStruct, Spring Web, Validation
                    3. Paquete base: com.example.project
                    4. Para cada entidad genera:
                       - Entity.java (JPA con @Entity, @Table, @Id, @GeneratedValue, etc.)
                       - Dto.java (clases DTO simples con Lombok)
                       - Repository.java (interface que extiende JpaRepository)
                       - Service.java (interface del servicio)
                       - ServiceImpl.java (implementación del servicio con @Service)
                       - Controller.java (REST controller con @RestController, @RequestMapping)
                       - Mapper.java (MapStruct interface con @Mapper)
                    
                    FORMATO DE RESPUESTA REQUERIDO:
                    Devuelve ÚNICAMENTE un JSON válido con esta estructura exacta:
                    {
                      "EntityName.java": "package com.example.project.entity;// código de la entidad...",
                      "EntityNameDto.java": "package com.example.project.dto;// código del DTO...",
                      "EntityNameRepository.java": "package com.example.project.repository;// código del repository...",
                      "EntityNameService.java": "package com.example.project.service;// código del service...",
                      "EntityNameServiceImpl.java": "package com.example.project.service.impl;// código del serviceImpl...",
                      "EntityNameController.java": "package com.example.project.controller;// código del controller...",
                      "EntityNameMapper.java": "package com.example.project.mapper;// código del mapper..."
                    }
                    
                    REGLAS CRÍTICAS:
                    - Las claves del JSON deben ser SOLO el nombre del archivo (ejemplo: "User.java", NO "com/example/User.java")
                    - Cada código debe empezar con el package correcto
                    - Usa @Data, @NoArgsConstructor, @AllArgsConstructor para entidades
                    - Usa @RestController, @RequestMapping, @GetMapping, @PostMapping, etc.
                    - Usa @Service, @RequiredArgsConstructor para servicios
                    - Usa @Repository para repositorios (opcional, pero buena práctica)
                    - Usa @Mapper(componentModel = "spring") para MapStruct
                    - NO incluyas explicaciones, comentarios extra o texto fuera del JSON
                    - NO uses markdown code blocks (```json), solo devuelve el JSON puro
                    
                    EJEMPLO ESPERADO para una entidad "User":
                    {
                      "User.java": "
                      package com.example.project.entity;
                      import jakarta.persistence.*;
                      import lombok.Data;
                      
                      @Entity
                      @Table(name = "users")
                      @Data
                      public class User {    
                            @Id
                            @GeneratedValue(strategy = GenerationType.IDENTITY)
                            private Long id;
                            private String name;
                      }",
                      
                      "UserDto.java": "
                      package com.example.project.dto;
                      
                      import lombok.Data;
                      
                      @Data
                      public class UserDto {
                            private Long id;
                            private String name;
                      }"
                    }
                    """.formatted(erdJson);

            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of(
                                    "parts", List.of(
                                            Map.of("text", promptText)
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

            JsonNode responseJson = objectMapper.readTree(response.getBody());
            JsonNode candidatesNode = responseJson.get("candidates");

            if (candidatesNode != null && !candidatesNode.isEmpty()) {
                JsonNode contentNode = candidatesNode.get(0).get("content");
                if (contentNode != null) {
                    JsonNode partsNode = contentNode.get("parts");
                    if (partsNode != null && !partsNode.isEmpty()) {
                        String generatedText = partsNode.get(0).get("text").asText();
                        log.debug("Raw Gemini response length: {}", generatedText.length());

                        String jsonContent = extractJsonFromResponse(generatedText);
                        log.debug("Extracted JSON length: {}", jsonContent.length());

                        Map<String, String> generatedFiles = objectMapper.readValue(jsonContent, new TypeReference<Map<String, String>>() {});

                        // CRÍTICO: Limpiar las claves para asegurar que solo sean nombres de archivos
                        Map<String, String> cleanedFiles = cleanFileNames(generatedFiles);

                        log.info("Successfully generated {} files", cleanedFiles.size());
                        cleanedFiles.keySet().forEach(filename -> log.debug("Generated file: {}", filename));

                        return cleanedFiles;
                    }
                }
            }

            throw new RuntimeException("No valid response received from Gemini API");

        } catch (Exception e) {
            log.error("Error calling Gemini API", e);
            throw new RuntimeException("Error generating code with Gemini: " + e.getMessage(), e);
        }
    }

    private String extractJsonFromResponse(String response) {
        String cleaned = response.trim();
        log.debug("Extracting JSON from response: {}", cleaned.substring(0, Math.min(200, cleaned.length())));

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

    /**
     * MÉTODO CRÍTICO: Limpia los nombres de archivos para evitar rutas duplicadas
     */
    private Map<String, String> cleanFileNames(Map<String, String> originalFiles) {
        Map<String, String> cleanedFiles = new HashMap<>();

        for (Map.Entry<String, String> entry : originalFiles.entrySet()) {
            String originalKey = entry.getKey();
            String content = entry.getValue();

            // Limpiar la clave: extraer solo el nombre del archivo
            String cleanKey = extractFileName(originalKey);

            log.debug("Cleaning key: '{}' -> '{}'", originalKey, cleanKey);

            // Verificar que el contenido no tenga rutas duplicadas en imports o packages
            String cleanContent = cleanPackageDeclarations(content);

            cleanedFiles.put(cleanKey, cleanContent);
        }

        return cleanedFiles;
    }

    private String extractFileName(String key) {
        // Si la clave contiene rutas, extraer solo el nombre del archivo
        if (key.contains("/")) {
            String[] parts = key.split("/");
            return parts[parts.length - 1]; // Tomar la última parte
        }

        // Si contiene backslashes (Windows)
        if (key.contains("\\")) {
            String[] parts = key.split("\\\\");
            return parts[parts.length - 1]; // Tomar la última parte
        }

        return key.trim();
    }

    private String cleanPackageDeclarations(String content) {
        // Asegurar que el package sea correcto
        if (content.contains("package ")) {
            // Buscar líneas de package duplicadas o incorrectas
            String[] lines = content.split("\\n");
            StringBuilder cleanContent = new StringBuilder();
            boolean packageFound = false;

            for (String line : lines) {
                if (line.trim().startsWith("package ")) {
                    if (!packageFound && line.contains("com.example.project")) {
                        cleanContent.append(line).append("\\n");
                        packageFound = true;
                    }
                    // Ignorar packages duplicados o incorrectos
                } else {
                    cleanContent.append(line).append("\\n");
                }
            }

            return cleanContent.toString();
        }

        return content;
    }
}