package com.example.capture_the_moment.repository.project;

import com.example.capture_the_moment.entity.project.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {
}
