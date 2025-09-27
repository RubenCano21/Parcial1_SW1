package org.example.schemaflow.shared.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class NodeDataRequest {

    private  String id;
    private String name;
    private String color;

    // Para clases UML
    private Boolean isAbstract;
    private Boolean isInterface;
    private List<AttributeRequest> attributes;
    //private List<MethodRequest> methods;

    // Para entidades ER (si las hay)
    private String tableName;
}
