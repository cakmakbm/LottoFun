package com.bmc.lottofun.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.bmc.lottofun.entity.User;

import java.math.BigDecimal;
import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);


    boolean existsByUsername(String username);

    /**
     * Atomically deducts {@code amount} only if the balance covers it.
     * The check and the update run as one SQL statement, so concurrent
     * purchases cannot both pass the check and overdraw the account.
     *
     * @return number of updated rows: 1 on success, 0 if the balance was insufficient
     */
    @Modifying
    @Query("UPDATE User u SET u.balance = u.balance - :amount WHERE u.id = :id AND u.balance >= :amount")
    int deductBalanceIfSufficient(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /**
     * Atomically adds {@code amount} to the balance, without a read-modify-write
     * in Java that could lose a concurrent update.
     */
    @Modifying
    @Query("UPDATE User u SET u.balance = u.balance + :amount WHERE u.id = :id")
    int addBalance(@Param("id") Long id, @Param("amount") BigDecimal amount);
}
