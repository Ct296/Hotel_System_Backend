package com.hotel.system.repository;

import com.hotel.system.entity.Account;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {

    Optional<Account> findById(String userId);

    @Override
    @EntityGraph(attributePaths = {"user"})
    List<Account> findAll();
}