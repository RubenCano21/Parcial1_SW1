package org.example.schemaflow.shared.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.schemaflow.shared.domain.dto.AttributeSchema;
import org.example.schemaflow.shared.domain.dto.EntitySchema;
import org.example.schemaflow.shared.domain.dto.RelationshipSchema;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ERDParserService {

    private  final ObjectMapper  objectMapper = new ObjectMapper();


    public String parseERDToStructuredPrompt (String frontendJson) {
        try {
            JsonNode rootNode = objectMapper.readTree(frontendJson);

            // Extraer entidades
            List<EntitySchema> entities = parseEntities(rootNode.get("nodes"));

            // Extraer relaciones
            List<RelationshipSchema> relationships = parseRelationships(rootNode.get("edges"), entities);

            // crear un prompt estructurado
            return buildStructuredPrompt(entities, relationships);
        } catch (Exception e) {
            log.error("Error parsing ERD JSON:", e);
            throw new RuntimeException("Failed top parse ERD JSON" + e.getMessage());
        }
    }

    private List<EntitySchema> parseEntities(JsonNode nodesArray) {
        List<EntitySchema> entities = new ArrayList<>();

        if (nodesArray != null && nodesArray.isArray()) {
            for (JsonNode node : nodesArray) {
                if ("entity".equals(node.get("type").asText())) {
                    JsonNode data = node.get("data");

                    EntitySchema entitySchema = new EntitySchema();
                    entitySchema.setId(data.get("id").asText());
                    entitySchema.setName(data.get("name").asText());
                    entitySchema.setColor(data.get("color").asText());

                    // Parse attributes
                    List<AttributeSchema> attributes = new ArrayList<>();
                    JsonNode attributesArray = data.get("attributes");

                    if (attributesArray != null && attributesArray.isArray()) {
                        for (JsonNode attrNode : attributesArray) {
                            AttributeSchema attribute = new AttributeSchema();
                            attribute.setId(attrNode.get("id").asText());
                            attribute.setName(attrNode.get("name").asText());
                            attribute.setType(attrNode.get("type").asText());
                            attribute.setPrimaryKey(attrNode.get("isPrimaryKey").asBoolean());
                            attribute.setForeignKey(attrNode.get("isForeignKey").asBoolean());
                            attribute.setRequired(attrNode.get("isRequired").asBoolean());
                            attributes.add(attribute);
                        }
                    }

                    entitySchema.setAttributes(attributes);
                    entities.add(entitySchema);

                    log.debug("Parsed entity: {} with {} attributes", entitySchema.getName(), attributes.size());
                }
            }
        }
        return entities;
    }

    private List<RelationshipSchema> parseRelationships(JsonNode edgesArray, List<EntitySchema> entities) {
        List<RelationshipSchema> relationships = new ArrayList<>();

        // Crear mapa de ID a nombre de entidad para referencias rápidas
        Map<String, String> entityIdToName = entities.stream()
                .collect(Collectors.toMap(EntitySchema::getId, EntitySchema::getName));

        if (edgesArray != null && edgesArray.isArray()) {
            for (JsonNode edge : edgesArray) {
                if ("relationship".equals(edge.get("type").asText())) {
                    JsonNode data = edge.get("data");

                    RelationshipSchema relationship = new RelationshipSchema();
                    relationship.setId(edge.get("id").asText());
                    relationship.setSourceEntityId(edge.get("source").asText());
                    relationship.setTargetEntityId(edge.get("target").asText());
                    relationship.setSourceEntityName(entityIdToName.get(edge.get("source").asText()));
                    relationship.setTargetEntityName(entityIdToName.get(edge.get("target").asText()));
                    relationship.setType(data.get("type").asText());
                    relationship.setLabel(data.get("label").asText());
                    relationship.setSourceCardinality(data.get("sourceCardinality").asText());
                    relationship.setTargetCardinality(data.get("targetCardinality").asText());

                    relationships.add(relationship);

                    log.debug("Parsed relationship: {} {} {} ({}:{})",
                            relationship.getSourceEntityName(),
                            relationship.getLabel(),
                            relationship.getTargetEntityName(),
                            relationship.getSourceCardinality(),
                            relationship.getTargetCardinality());
                }
            }
        }

        return relationships;
    }

    private String buildStructuredPrompt(List<EntitySchema> entities, List<RelationshipSchema> relationships) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("SISTEMA DE GENERACIÓN DE CÓDIGO SPRING BOOT\n\n");

        // Sección de entidades
        prompt.append("ENTIDADES A GENERAR:\n");
        for (EntitySchema entity : entities) {
            prompt.append(String.format("- %s:\n", entity.getName()));
            for (AttributeSchema attr : entity.getAttributes()) {
                String constraints = buildConstraints(attr);
                prompt.append(String.format("  * %s: %s%s\n",
                        attr.getName(),
                        mapToJavaType(attr.getType()),
                        constraints));
            }
            prompt.append("\n");
        }

        // Sección de relaciones
        if (!relationships.isEmpty()) {
            prompt.append("RELACIONES:\n");
            for (RelationshipSchema rel : relationships) {
                String jpaRelation = mapToJPARelation(rel);
                prompt.append(String.format("- %s %s %s (%s:%s) -> %s\n",
                        rel.getSourceEntityName(),
                        rel.getLabel(),
                        rel.getTargetEntityName(),
                        rel.getSourceCardinality(),
                        rel.getTargetCardinality(),
                        jpaRelation));
            }
            prompt.append("\n");
        }

        // Instrucciones específicas
        prompt.append("""
                INSTRUCCIONES DE GENERACIÓN:
                1. Generar entidades JPA con las relaciones correctas
                2. Usar @OneToOne, @OneToMany, @ManyToOne, @ManyToMany según corresponda
                3. Implementar DTOs sin relaciones circulares
                4. Crear repositorios JPA con métodos de búsqueda apropiados
                5. Implementar servicios con operaciones CRUD completas
                6. Generar controllers REST con endpoints estándar
                7. Crear mappers MapStruct que manejen las relaciones correctamente
                8. Usar Lombok para reducir boilerplate code
                9. Implementar validaciones apropiadas (@NotNull, @Size, etc.)
                10. Usar nombres en inglés para clases y métodos, español para comentarios
                """);

        return prompt.toString();
    }

    private String buildConstraints(AttributeSchema attr) {
        List<String> constraints = new ArrayList<>();

        if (attr.isPrimaryKey()) constraints.add("PRIMARY KEY");
        if (attr.isForeignKey()) constraints.add("FOREIGN KEY");
        if (attr.isRequired()) constraints.add("NOT NULL");

        return constraints.isEmpty() ? "" : " (" + String.join(", ", constraints) + ")";
    }

    private String mapToJavaType(String sqlType) {
        String upperType = sqlType.toUpperCase();

        if (upperType.equals("INTEGER")) return "Long";
        if (upperType.startsWith("VARCHAR")) return "String";
        return switch (upperType) {
            case "BOOLEAN" -> "Boolean";
            case "DATE" -> "LocalDate";
            case "DATETIME" -> "LocalDateTime";
            case "DECIMAL" -> "BigDecimal";
            case "FLOAT" -> "Double";
            default -> "String";
        };

    }

    private String mapToJPARelation(RelationshipSchema rel) {
        String sourceCard = rel.getSourceCardinality();
        String targetCard = rel.getTargetCardinality();
        String type = rel.getType().toLowerCase();

        // Mapeo basado en cardinalidades
        if ("1".equals(sourceCard) && "1".equals(targetCard)) {
            return "@OneToOne";
        } else if ("1".equals(sourceCard) && "*".equals(targetCard)) {
            return "@OneToMany";
        } else if ("*".equals(sourceCard) && "1".equals(targetCard)) {
            return "@ManyToOne";
        } else if ("*".equals(sourceCard) && "*".equals(targetCard)) {
            return "@ManyToMany";
        } else if (type.contains("many-to-many") || "1..*".equals(sourceCard) && "1..*".equals(targetCard)) {
            return "@ManyToMany";
        } else if ("1..*".equals(targetCard) || "*".equals(targetCard)) {
            return "@OneToMany";
        }

        return "@ManyToOne"; // Default fallback
    }


}
