package org.example.schemaflow.shared.presentation.controller;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.schemaflow.shared.application.service.FileWriterService;
import org.example.schemaflow.shared.application.service.GeminiService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {
        "http://localhost:3000",
        "http://127.0.0.1:3000",
        "https://sw1-er-diagram-front.onrender.com"
}, maxAge = 3600)
public class GeneratorController {

    private final GeminiService geminiService;
    private final FileWriterService fileWriterService;

    @PostMapping("/generator")
    public ResponseEntity<?> generateProject(@RequestBody GenerateRequest request) {
        log.info("Received project generation request: {}", request.getProjectName());

        try {
            // Validaciones de entrada
            if (request.getErdJson() == null || request.getErdJson().trim().isEmpty()) {
                log.warn("Empty ERD JSON received");
                return ResponseEntity.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of(
                                "error", "ERD_JSON_REQUIRED",
                                "message", "ERD JSON is required and cannot be empty"
                        ));
            }

            if (request.getProjectName() == null || request.getProjectName().trim().isEmpty()) {
                log.warn("Empty project name received");
                return ResponseEntity.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of(
                                "error", "PROJECT_NAME_REQUIRED",
                                "message", "Project name is required and cannot be empty"
                        ));
            }

            // Sanitizar nombre del proyecto
            String sanitizedProjectName = sanitizeProjectName(request.getProjectName());
            log.info("Generating project: {} (sanitized: {})", request.getProjectName(), sanitizedProjectName);

            // 1. Generar código usando Gemini
            log.info("Requesting code generation from Gemini service");
            Map<String, String> generatedFiles = geminiService.generateCode(request.getErdJson());

            if (generatedFiles == null || generatedFiles.isEmpty()) {
                log.error("Gemini service returned no generated files");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of(
                                "error", "CODE_GENERATION_FAILED",
                                "message", "No files were generated. Please check your ERD data."
                        ));
            }

            log.info("Generated {} files from Gemini service", generatedFiles.size());

            // 2. Crear ZIP con archivos y estructura del proyecto
            byte[] zipBytes = fileWriterService.createProjectZip(generatedFiles, sanitizedProjectName);

            if (zipBytes == null || zipBytes.length == 0) {
                log.error("Failed to create project ZIP file");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of(
                                "error", "ZIP_CREATION_FAILED",
                                "message", "Failed to create project ZIP file"
                        ));
            }

            // 3. Preparar respuesta de descarga
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("%s_%s.zip", sanitizedProjectName, timestamp);

            log.info("Successfully generated project ZIP: {} ({} bytes)", filename, zipBytes.length);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(zipBytes.length);
            headers.add("X-Generated-Project", sanitizedProjectName);
            headers.add("X-Generated-Files", String.valueOf(generatedFiles.size()));

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(zipBytes);

        } catch (IllegalArgumentException e) {
            log.error("Invalid input data: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "error", "INVALID_INPUT",
                            "message", "Invalid input data: " + e.getMessage()
                    ));

        } catch (Exception e) {
            log.error("Unexpected error during project generation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "error", "INTERNAL_SERVER_ERROR",
                            "message", "An unexpected error occurred: " + e.getMessage()
                    ));
        }
    }

    // Mantener el endpoint legacy por compatibilidad
    @PostMapping("/generator/v1")
    public ResponseEntity<byte[]> generatedLegacy(@RequestBody GenerateRequest request) {
        log.info("Legacy endpoint called, redirecting to new implementation");
        ResponseEntity<?> response = generateProject(request);

        if (response.getBody() instanceof byte[]) {
            return ResponseEntity.status(response.getStatusCode())
                    .headers(response.getHeaders())
                    .body((byte[]) response.getBody());
        } else {
            // Si hay error, convertir a respuesta legacy
            String errorMessage = "Error: " + response.getBody().toString();
            return ResponseEntity.status(response.getStatusCode())
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(errorMessage.getBytes());
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        log.debug("Health check requested");
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "ERD Code Generator",
                "timestamp", LocalDateTime.now().toString(),
                "version", "1.0.0"
        ));
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getServiceInfo() {
        log.debug("Service info requested");
        return ResponseEntity.ok(Map.of(
                "name", "ERD Code Generator API",
                "description", "Generates Spring Boot projects from Entity-Relationship Diagrams using AI",
                "version", "1.0.0",
                "endpoints", Map.of(
                        "generate", "/api/generator",
                        "health", "/api/health",
                        "info", "/api/info"
                ),
                "supportedFormats", new String[]{"JSON"},
                "outputFormat", "ZIP file with complete Spring Boot project"
        ));
    }

    // Método para OPTIONS (necesario para CORS preflight)
    @RequestMapping(method = RequestMethod.OPTIONS, value = "/generator")
    public ResponseEntity<Void> handleOptions() {
        return ResponseEntity.ok()
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "POST, GET, OPTIONS")
                .header("Access-Control-Allow-Headers", "Content-Type, Authorization")
                .build();
    }

    private String sanitizeProjectName(String projectName) {
        if (projectName == null || projectName.trim().isEmpty()) {
            return "generated-project";
        }

        return projectName.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "") // Solo letras, números, espacios y guiones
                .replaceAll("\\s+", "-")         // Espacios a guiones
                .replaceAll("-+", "-")           // Múltiples guiones a uno solo
                .replaceAll("^-|-$", "");        // Remover guiones al inicio y final
    }

    // Clase interna para el request body (mejorada)
    @Setter
    @Getter
    public static class GenerateRequest {
        // Getters y Setters
        private String erdJson;
        private String projectName;

        // Constructors
        public GenerateRequest() {}

        public GenerateRequest(String erdJson, String projectName) {
            this.erdJson = erdJson;
            this.projectName = projectName;
        }

        @Override
        public String toString() {
            return "GenerateRequest{" +
                    "projectName='" + projectName + '\'' +
                    ", erdJsonLength=" + (erdJson != null ? erdJson.length() : 0) +
                    '}';
        }
    }
}