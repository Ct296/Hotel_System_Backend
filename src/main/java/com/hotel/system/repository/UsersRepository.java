package com.hotel.system.repository;

import com.hotel.system.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsersRepository extends JpaRepository<Users, String> {
    Optional<Users> findByEmail(String email);
    Optional<Users> findByPid(String pid);
    Optional<Users> findByPhoneNumber(String phoneNumber);
}