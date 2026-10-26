package com.example.capture_the_moment.repository.project;

import com.example.capture_the_moment.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByProjectProjectId(Long projectId);

    void deleteByProjectProjectId(Long projectId);
}
