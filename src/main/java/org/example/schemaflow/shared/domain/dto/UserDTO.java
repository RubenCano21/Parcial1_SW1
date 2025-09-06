package org.example.schemaflow.shared.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserDTO {

    private Long id;
    private String username;
    private String role;

    public UserDTO() {
    }

}
