package com.bmc.lottofun.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Getter
@Setter
public class UserLoginRequest {

    @NotBlank(message = "User name can not be empty")
    private String username;

    @NotBlank(message = "Password can not be empty")
    private String password;


}
