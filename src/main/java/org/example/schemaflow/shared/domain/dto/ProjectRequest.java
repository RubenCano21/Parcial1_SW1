package org.example.schemaflow.shared.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProjectRequest {

    private String name;
    private String version;
    private String createdAt;
//    private List<NodeRequest> nodes;
//    private List<EdgeRequest> edges;
}
