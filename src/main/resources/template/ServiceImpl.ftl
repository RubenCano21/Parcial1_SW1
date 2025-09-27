package ${package}.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

import ${package}.repository.${table.className}Repository;
import ${package}.mapper.${table.className}Mapper;
import ${package}.dto.${table.className}Dto;
import ${package}.entity.${table.className};
import ${package}.service.${table.className}Service;

@Service
@RequiredArgsConstructor
public class ${table.className}ServiceImpl implements ${table.className}Service {

    private final ${table.className}Repository repository;
    private final ${table.className}Mapper mapper;

    @Override
    public ${table.className}Dto create(${table.className}Dto dto) {
        return mapper.toDto(repository.save(mapper.toEntity(dto)));
    }

    @Override
    public ${table.className}Dto update(Long id, ${table.className}Dto dto) {
        ${table.className} entity = repository.findById(id)
        .orElseThrow(() -> new RuntimeException("${table.className} not found"));
        dto.setId(id);
        return mapper.toDto(repository.save(mapper.toEntity(dto)));
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    public ${table.className}Dto getById(Long id) {
        return repository.findById(id)
        .map(mapper::toDto)
        .orElseThrow(() -> new RuntimeException("${table.className} not found"));
    }

    @Override
    public List<${table.className}Dto> getAll() {
        return repository.findAll()
        .stream()
        .map(mapper::toDto)
        .collect(Collectors.toList());
    }
}