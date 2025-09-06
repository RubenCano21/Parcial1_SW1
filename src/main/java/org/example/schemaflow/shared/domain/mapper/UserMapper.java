package org.example.schemaflow.shared.domain.mapper;

import org.example.schemaflow.shared.domain.dto.UserDTO;
import org.example.schemaflow.shared.domain.entities.Role;
import org.example.schemaflow.shared.domain.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "roles", target = "role", qualifiedByName = "getFirstRoleName")
    UserDTO toDto(User user);

    @Named("getFirstRoleName")
    default String getFirstRoleName(List<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        return roles.getFirst().getName().name();
    }

    List<UserDTO> toDtoList(List<User> users);


    User toEntity(UserDTO dto);
    List<User> toEntityList(List<UserDTO> dtos);
}