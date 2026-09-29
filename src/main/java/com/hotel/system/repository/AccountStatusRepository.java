package com.hotel.system.repository;

import com.hotel.system.entity.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountStatusRepository extends JpaRepository<AccountStatus, String> {

    Optional<AccountStatus> findTopByAccountIdOrderByStartTimeDesc(String accountId);
}