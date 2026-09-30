package com.hcb.repository;

import com.hcb.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByMobile(String mobile);

    Optional<User> findByEmailOrMobile(String email, String mobile);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.mobile LIKE CONCAT('%', :tenDigit)")
    Optional<User> findByMobileFuzzy(@org.springframework.data.repository.query.Param("tenDigit") String tenDigit);

    boolean existsByEmail(String email);

    boolean existsByMobile(String mobile);
}
