package com.bmc.lottofun;

import com.bmc.lottofun.entity.User;
import com.bmc.lottofun.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;


@Slf4j
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class LottoFunApplication {

    public static void main(String[] args) {

        SpringApplication.run(LottoFunApplication.class, args);
    }

    @Bean
    CommandLineRunner initAdmin(UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                @Value("${lotto.admin.password:}") String adminPassword) {
        return args -> {
            if (userRepository.existsByUsername("admin")) {
                return;
            }
            if (adminPassword == null || adminPassword.isBlank()) {
                log.warn("ADMIN_PASSWORD is not set, so no admin account was created.");
                return;
            }
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode(adminPassword))
                    .balance(BigDecimal.ZERO)
                    .role("ROLE_ADMIN")
                    .build();
            try {
                userRepository.save(admin);
                log.info("Admin account created.");
            } catch (DataIntegrityViolationException e) {
                // Several replicas can start at once; another one created the admin first.
                log.info("Admin account was created by another instance.");
            }
        };
    }

}
