package ${package}.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ${table.className}Dto {
<#list table.columns as col>
    private ${col.foreignKey?then(col.foreignKey.refClass + "Dto", col.javaType)} ${col.javaField};
</#list>
}
