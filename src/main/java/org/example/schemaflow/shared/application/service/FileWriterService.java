package org.example.schemaflow.shared.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Slf4j
public class FileWriterService {

    // Constantes para evitar duplicaciones
    private static final String JAVA_BASE = "src/main/java";
    private static final String PACKAGE_PATH = "com/example/project";
    private static final String RESOURCES_BASE = "src/main/resources";

    public byte[] createProjectZip(Map<String, String> files, String projectName) throws IOException {
        log.info("Creating project ZIP for: {}", projectName);
        log.info("Number of files to process: {}", files.size());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipOutputStream zos = new ZipOutputStream(baos)) {

            // 1. Procesar archivos generados por Gemini
            for (Map.Entry<String, String> entry : files.entrySet()) {
                String filename = entry.getKey();
                String content = entry.getValue();

                log.debug("Processing file: {}", filename);

                // Construir ruta completa sin duplicaciones
                String zipPath = buildFilePath(filename);
                log.debug("Final ZIP path: {}", zipPath);

                addFileToZip(zos, zipPath, content);
            }

            // 2. Agregar archivos de configuración del proyecto
            addProjectFiles(zos, projectName);

        }

        byte[] result = baos.toByteArray();
        log.info("ZIP created successfully. Size: {} bytes", result.length);
        return result;
    }

    /**
     * Construye la ruta completa del archivo basándose solo en el nombre del archivo
     */
    private String buildFilePath(String filename) {
        String folder = getPackageFolder(filename);
        String fullPath = JAVA_BASE + "/" + PACKAGE_PATH + "/" + folder + "/" + filename;

        log.debug("File: {} -> Folder: {} -> Path: {}", filename, folder, fullPath);
        return fullPath;
    }

    /**
     * Determina la carpeta del paquete basándose en el sufijo del archivo
     */
    private String getPackageFolder(String filename) {
        if (filename.endsWith("Dto.java")) return "dto";
        if (filename.endsWith("Mapper.java")) return "mapper";
        if (filename.endsWith("Repository.java")) return "repository";
        if (filename.endsWith("Service.java")) return "service";
        if (filename.endsWith("ServiceImpl.java")) return "service/impl";
        if (filename.endsWith("Controller.java")) return "controller";
        return "entity"; // Por defecto para entidades
    }

    private void addFileToZip(ZipOutputStream zos, String filePath, String content) throws IOException {
        // LOG CRÍTICO: Aquí vemos exactamente qué se está agregando al ZIP
        log.info("ADDING TO ZIP: {}", filePath);

        // SOLUCIÓN: Convertir los \n literales a saltos de línea reales
        String cleanContent = unescapeContent(content);

        ZipEntry entry = new ZipEntry(filePath);
        zos.putNextEntry(entry);
        zos.write(cleanContent.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    /**
     * Convierte los caracteres de escape literales a caracteres reales
     */
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

    private void addProjectFiles(ZipOutputStream zos, String projectName) throws IOException {
        log.info("Adding project configuration files");

        // Archivos de configuración con rutas absolutas (sin concatenación)
        addFileToZip(zos, "pom.xml", generatePomXml(projectName));
        addFileToZip(zos, ".gitignore", generateGitignore());
        addFileToZip(zos, "README.md", generateReadme(projectName));
        addFileToZip(zos, ".env", generateEnv());

        // Application.yml en resources
        addFileToZip(zos, RESOURCES_BASE + "/application.properties", generateApplicationProperties(projectName));

        // Clase principal (RUTA FIJA - SIN CONCATENACIÓN)
        String mainClassPath = JAVA_BASE + "/" + PACKAGE_PATH + "/Application.java";
        addFileToZip(zos, mainClassPath, generateMainClass(projectName));

        log.info("Project configuration files added successfully");
    }

    // Métodos de generación de contenido (sin cambios en la lógica)
    private String generatePomXml(String projectName) {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0" 
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                     https://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>3.2.0</version>
                    <relativePath/>
                </parent>
                <groupId>com.example</groupId>
                <artifactId>%s</artifactId>
                <version>0.0.1-SNAPSHOT</version>
                <name>%s</name>
                <description>Generated Spring Boot project</description>
                <properties>
                    <java.version>17</java.version>
                    <mapstruct.version>1.5.5.Final</mapstruct.version>
                </properties>
                <dependencies>
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-web</artifactId>
                    </dependency>
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-data-jpa</artifactId>
                    </dependency>
                    <dependency>
                        <groupId>com.h2database</groupId>
                        <artifactId>h2</artifactId>
                        <scope>runtime</scope>
                    </dependency>
                    <dependency>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <optional>true</optional>
                    </dependency>
                    <dependency>
                        <groupId>org.mapstruct</groupId>
                        <artifactId>mapstruct</artifactId>
                        <version>${mapstruct.version}</version>
                    </dependency>
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-test</artifactId>
                        <scope>test</scope>
                    </dependency>
                </dependencies>
                <build>
                    <plugins>
                        <plugin>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-maven-plugin</artifactId>
                        </plugin>
                    </plugins>
                </build>
            </project>
            """.formatted(projectName.toLowerCase(), projectName);
    }

    private String generateGitignore() {
        return """
            HELP.md
            target/
            !.mvn/wrapper/maven-wrapper.jar
            !**/src/main/**/target/
            !**/src/test/**/target/
            
            ### STS ###
            .apt_generated
            .classpath
            .factorypath
            .project
            .settings
            .springBeans
            .sts4-cache
            
            ### IntelliJ IDEA ###
            .idea
            *.iws
            *.iml
            *.ipr
            
            ### NetBeans ###
            /nbproject/private/
            /nbbuild/
            /dist/
            /nbdist/
            /.nb-gradle/
            build/
            !**/src/main/**/build/
            !**/src/test/**/build/
            
            ### VS Code ###
            .vscode/
            """;
    }

    private String generateReadme(String projectName) {
        return """
            # %s
            
            Proyecto Spring Boot generado automáticamente.
            
            ## Instrucciones
            
            1. Importar en tu IDE
            2. Ejecutar: `./mvnw spring-boot:run`
            3. Acceder a: http://localhost:8080
            
            """.formatted(projectName);
    }

    private String generateEnv(){
        return """
            SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/diagramador
            SPRING_DATASOURCE_USERNAME=postgres
            SPRING_DATASOURCE_PASSWORD=6784
            SPRING_JPA_HIBERNATE_DDL_AUTO=update
            """;
    }

    private String generateApplicationProperties(String projectName) {
        return """
                spring.datasource.url=jdbc:postgresql://localhost:5432/diagramador
                spring.datasource.username=postgres
                spring.datasource.password=6784
                spring.jpa.hibernate.ddl-auto=update
            """.formatted(projectName);
    }

    private String generateMainClass(String projectName) {
        String className = toPascalCase(projectName) + "Application";
        return """
            package com.example.project;
            
            import org.springframework.boot.SpringApplication;
            import org.springframework.boot.autoconfigure.SpringBootApplication;
            
            @SpringBootApplication
            public class %s {
                
                public static void main(String[] args) {
                    SpringApplication.run(%s.class, args);
                }
            }
            """.formatted(className, className);
    }

    private String toPascalCase(String input) {
        if (input == null || input.isEmpty()) return "Generated";

        return input.substring(0, 1).toUpperCase() +
                input.substring(1).toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
    }
}