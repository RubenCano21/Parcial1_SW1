package org.example.schemaflow.shared.application.service;

import org.example.schemaflow.shared.domain.dto.EntitySchema;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;

@Service
public class ProjectGeneratorService {

    private final GeminiService geminiService;

    public ProjectGeneratorService(GeminiService geminiService) {
        this.geminiService = geminiService;
    }



    public void generateCodeForEntity(EntitySchema entity, String packageBase, File projectDir) throws IOException {
        String prompt = """
                Genera código Java para una entidad llamada %s, usando:
                - JPA (@Entity, @Id, @GeneratedValue)
                - Lombok (@Data, @NoArgsConstructor, @AllArgsConstructor, @Builder)
                - Repository con JPARepository
                - Service con CRUD
                - Controller con endpoints REST
                Paquete base: %s
                Atributos: %s
                Responde con bloques separados:
                === ENTITY ===
                === REPOSITORY ===
                === SERVICE ===
                === SERVICEIMPL ===
                === CONTROLLER ===
                === DTO ===
                Cada bloque debe contener solo el código Java, sin explicaciones ni texto adicional.
                """
                .formatted(entity.getName(), packageBase, entity.getAttributes());

        String aiResponse = geminiService.generateCode(prompt).toString();

        saveGeneratedFiles(aiResponse, entity.getName(), packageBase, projectDir);
    }

    private void saveGeneratedFiles(String aiResponse, String entityName, String packageBase, File projectDir) throws IOException {
        String basePath = projectDir.getAbsolutePath() + "/src/main/java/" + packageBase.replace(".", "/");

        Map<String, String> sections = Map.of(
                "ENTITY", "/entity/" + entityName + ".java",
                "REPOSITORY", "/repository/" + entityName + "Repository.java",
                "SERVICE", "/service/" + entityName + "Service.java",
                "SERVICEIMPL", "/service/impl/" + entityName + "ServiceImpl.java",
                "CONTROLLER", "/controller/" + entityName + "Controller.java",
                "DTO", "/dto/" + entityName + "DTO.java"
        );

        for (Map.Entry<String, String> entry : sections.entrySet()) {
            String section = extractSection(aiResponse, entry.getKey());
            if (section != null) {
                Path path = Paths.get(basePath + entry.getValue());
                Files.createDirectories(path.getParent());
                Files.writeString(path, section, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
        }
    }

    private String extractSection(String text, String sectionName) {
        String marker = "=== " + sectionName + " ===";
        int start = text.indexOf(marker);
        if (start == -1) return null;
        start += marker.length();
        int end = text.indexOf("===", start);
        if (end == -1) end = text.length();
        return text.substring(start, end).trim();
    }
}

