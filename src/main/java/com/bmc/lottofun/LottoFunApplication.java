package com.bmc.lottofun;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;


@SpringBootApplication
@EnableAsync
public class LottoFunApplication {

    public static void main(String[] args) {

        SpringApplication.run(LottoFunApplication.class, args);
    }

}
