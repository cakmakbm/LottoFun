package com.bmc.lottofun.dto.request;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;


@Data
@Getter
@Setter
public class UserRegisterRequest {

    @NotBlank(message = "User Name can not be emtpy")
    @Size(min=3, max =20, message = "User name must be 3-20 char")
    private String username;

    @NotBlank(message = "Password can not be empty")
    @Size(min=6, message = "Password must be at least six char")
    private String password;




}
