package com.bmc.lottofun;

import com.bmc.lottofun.entity.User;
import com.bmc.lottofun.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;


@SpringBootApplication
@EnableAsync
@EnableScheduling
public class LottoFunApplication {

    public static void main(String[] args) {

        SpringApplication.run(LottoFunApplication.class, args);
    }
    @Bean
    CommandLineRunner initAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (!userRepository.existsByUsername("admin")) {
                User admin = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin123"))
                        .balance(BigDecimal.ZERO)
                        .role("ROLE_ADMIN")
                        .build();
                userRepository.save(admin);
            }
        };
    }

}
