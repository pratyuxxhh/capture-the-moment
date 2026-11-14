package com.example.capture_the_moment.repository.kanban;

import com.example.capture_the_moment.entity.kanban.Kanban;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KanbanRepository extends JpaRepository<Kanban, Long> {
    Optional<Kanban> findByProjectProjectId(Long projectId);
}
