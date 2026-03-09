package com.vishnu.finance_tracker.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vishnu.finance_tracker.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);


}

