package ${package}.mapper;

import org.mapstruct.*;
import ${package}.entity.${table.className};
import ${package}.dto.${table.className}Dto;

@Mapper(componentModel = "spring")
public interface ${table.className}Mapper {
${table.className}Dto toDto(${table.className} entity);
${table.className} toEntity(${table.className}Dto dto);
}
