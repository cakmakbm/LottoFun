package com.bmc.lottofun.service;
import com.bmc.lottofun.exception.BadRequestException;
import com.bmc.lottofun.service.interfaces.ITicketValidator;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;


import java.util.HashSet;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TicketValidator implements ITicketValidator {

    @Value("${lottery.rule.number-count}")
    private int requiredNumberCount;

    @Value("${lottery.rule.min-number}")
    private int minNumber;

    @Value("${lottery.rule.max-number}")
    private int maxNumber;

    public void validate(List<Integer> numbers) {
        if (numbers.size() != requiredNumberCount) {
            throw new BadRequestException("Number count must be " + requiredNumberCount);
        }
        if (new HashSet<>(numbers).size() != numbers.size()) {
            throw new BadRequestException("Numbers must be unique");
        }
        for (Integer number : numbers) {
            if (number < minNumber || number > maxNumber) {
                throw new BadRequestException("Number " + number + " out of range [" + minNumber + "-" + maxNumber + "]");
            }
        }
    }
}
