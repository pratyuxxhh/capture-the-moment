package com.example.capture_the_moment.repository.user;

import com.example.capture_the_moment.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    boolean existsByUsername(String username);

    java.util.Optional<UserEntity> findByUsername(String username);
}
