package com.back.together02be.users.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.together02be.users.enitity.Users;

public interface UsersRepository extends JpaRepository<Users, Long> {
}
