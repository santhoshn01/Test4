package com.example.demo.repository;

import com.example.demo.domain.Users;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;



@Repository
public interface UserRepository extends JpaRepository<Users, Long> {}
