package ${package}.service;

import java.util.List;
import ${package}.dto.${table.className}Dto;

public interface ${table.className}Service {
    ${table.className}Dto create(${table.className}Dto dto);
    ${table.className}Dto update(Long id, ${table.className}Dto dto);
    void delete(Long id);
    ${table.className}Dto getById(Long id);
    List<${table.className}Dto> getAll();
}