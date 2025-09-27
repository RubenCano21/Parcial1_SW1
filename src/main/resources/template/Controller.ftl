package ${package}.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import ${package}.service.${table.className}Service;
import ${package}.dto.${table.className}Dto;

@RestController
@RequestMapping("/api/${table.name}")
@RequiredArgsConstructor
public class ${table.className}Controller {

    private final ${table.className}Service service;

    @PostMapping
    public ${table.className}Dto create(@RequestBody ${table.className}Dto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public ${table.className}Dto update(@PathVariable Long id, @RequestBody ${table.className}Dto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/{id}")
    public ${table.className}Dto getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping
    public List<${table.className}Dto> getAll() {
        return service.getAll();
    }
}