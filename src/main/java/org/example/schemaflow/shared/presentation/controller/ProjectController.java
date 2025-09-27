package org.example.schemaflow.shared.presentation.controller;

import lombok.RequiredArgsConstructor;
import org.example.schemaflow.shared.application.service.ProjectGeneratorService;
import org.example.schemaflow.shared.domain.dto.EntitySchema;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectGeneratorService projectGeneratorService;


//    public ResponseEntity<?> generateCode(EntitySchema schema, String packageBase, File projectDir) throws IOException {
//         return ResponseEntity.ok().body(projectGeneratorService.generateCodeForEntity(schema, packageBase, projectDir));
//    }

}
