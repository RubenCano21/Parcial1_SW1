package ${package}.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "${table.name}")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ${table.className} {

<#list table.columns as col>
    <#if col.primary?? && col.primary>
        @Id
        <#if col.autoIncrement?? && col.autoIncrement>
            @GeneratedValue(strategy = GenerationType.IDENTITY)
        </#if>
    </#if>
    <#if col.foreignKey??>
        @ManyToOne
        @JoinColumn(name = "${col.name}", nullable = ${col.nullable?string("true","false")})
        private ${col.foreignKey.refClass} ${col.javaField};
    <#else>
        @Column(
        <#if !col.nullable?? || !col.nullable>nullable = false</#if>
        <#if col.length??>, length = ${col.length}</#if>
        )
        private ${col.javaType} ${col.javaField};
    </#if>

</#list>
}
