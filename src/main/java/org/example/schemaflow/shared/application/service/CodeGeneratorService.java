package org.example.schemaflow.shared.application.service;

import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.RequiredArgsConstructor;
import org.example.schemaflow.shared.domain.dto.EntitySchema;
import org.example.schemaflow.shared.domain.dto.ProjectSchema;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CodeGeneratorService {

    private final Configuration freemarkerConfig;
    private final GeminiService geminiService;

    public void generateCodeFromJson(String erdJson) throws Exception {
        Map<String, String> files = geminiService.generateCode(erdJson);

        for (Map.Entry<String, String> entry : files.entrySet()) {
            String filename = entry.getKey();
            String content = entry.getValue();

            String folder = resolveFolder(filename);
            String filePath = "src/main/java/com/project/" + folder + "/" + filename;

            try (Writer writer = new FileWriter(filePath)) {
                writer.write(content);
            }
        }
    }

    private String resolveFolder(String filename) {
        if (filename.endsWith("Dto.java")) return "dto";
        if (filename.endsWith("Mapper.java")) return "mapper";
        if (filename.endsWith("Repository.java")) return "repository";
        if (filename.endsWith("Service.java")) return "service";
        if (filename.endsWith("ServiceImpl.java")) return "service/impl";
        if (filename.endsWith("Controller.java")) return "controller";
        return "entity";
    }

    public void generateCodeFromJson(ProjectSchema erd) throws Exception {
        for (EntitySchema table : erd.getEntities()) {
            Map<String, Object> ctx = new HashMap<>();
            ctx.put("package", "com.project");
            ctx.put("table", table);

            // entity
            writeFile("Entity.ftl", ctx, "src/main/java/com/project/entity/" + table.getName() + ".java");

            // dto
            writeFile("DTO.ftl", ctx, "src/main/java/com/project/dto/" + table.getName() + "Dto.java");

            // mapper
            writeFile("Mapper.ftl", ctx, "src/main/java/com/project/mapper/" + table.getName() + "Mapper.java");

            // repository
            writeFile("Repository.ftl", ctx, "src/main/java/com/project/repository/" + table.getName() + "Repository.java");

            // service
            writeFile("Service.ftl", ctx, "src/main/java/com/project/service/" + table.getName() + "Service.java");

            // service impl
            writeFile("ServiceImpl.ftl", ctx, "src/main/java/com/project/service/impl/" + table.getName() + "ServiceImpl.java");

            // controller
            writeFile("Controller.ftl", ctx, "src/main/java/com/project/controller/" + table.getName() + "Controller.java");
        }
    }

    private void writeFile(String templateName, Map<String, Object> ctx, String filePath) throws Exception {
        Template template = freemarkerConfig.getTemplate(templateName);
        try (Writer writer = new FileWriter(filePath)) {
            template.process(ctx, writer);
        }
    }
}

