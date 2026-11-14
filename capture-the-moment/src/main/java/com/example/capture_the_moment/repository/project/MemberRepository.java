package com.example.capture_the_moment.repository.project;

import com.example.capture_the_moment.entity.project.Member;
import com.example.capture_the_moment.entity.project.MemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, MemberId> {
    @EntityGraph(attributePaths = {"project", "project.createdBy"})
    List<Member> findByUserUsernameOrderByJoinedAtDesc(String username);

    @EntityGraph(attributePaths = {"project", "project.createdBy"})
    Optional<Member> findByProjectProjectIdAndUserUsername(Long projectId, String username);

    @EntityGraph(attributePaths = {"user"})
    List<Member> findByProjectProjectIdOrderByJoinedAtAsc(Long projectId);

    void deleteAllByProjectProjectId(Long projectId);
}
