package com.bmc.lottofun.service;


import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;


import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

@Service
public class NumberGeneratorService {

    @Value("${lottery.rule.number-count}")
    private int requiredNumberCount;

    @Value("${lottery.rule.min-number}")
    private int minNumber;

    @Value("${lottery.rule.max-number}")
    private int maxNumber;

    private final SecureRandom secureRandom = new SecureRandom();

    public Set<Integer> generateWinningNumbers(){
        Set<Integer> winningNumbers = new HashSet<>();

        while(winningNumbers.size() < requiredNumberCount){
            int randomNumber = secureRandom.nextInt((maxNumber - minNumber) + 1) + minNumber;

            winningNumbers.add(randomNumber);
        }
        return winningNumbers;
    }


}
