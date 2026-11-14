package com.example.capture_the_moment.repository.kanban;

import com.example.capture_the_moment.entity.kanban.Task;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
}
