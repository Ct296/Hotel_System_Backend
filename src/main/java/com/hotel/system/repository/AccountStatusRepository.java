package com.hotel.system.repository;

import com.hotel.system.entity.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountStatusRepository extends JpaRepository<AccountStatus, String> {

    Optional<AccountStatus> findTopByAccountIdOrderByStartTimeDesc(String accountId);

    // Dùng Account_User_Id để Spring Boot tự động bắc cầu từ AccountStatus -> Account -> Users
    List<AccountStatus> findByAccount_User_IdOrderByStartTimeDesc(String userId);

}