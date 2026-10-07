package com.example.capture_the_moment.controllers.project;

import com.example.capture_the_moment.entity.Member;
import com.example.capture_the_moment.entity.ProjectEntity;
import com.example.capture_the_moment.services.project.ProjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/project")
public class ProjectController {
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public List<ProjectResponse> getProjects(@AuthenticationPrincipal UserDetails user) {
        return projectService.findMembershipsForUser(user.getUsername()).stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@AuthenticationPrincipal UserDetails user,
                                                         @RequestBody ProjectRequest request) {
        ProjectEntity project = projectService.createProject(user.getUsername(), request.projectName());
        return ResponseEntity.status(201).body(ProjectResponse.from(project));
    }

    @PatchMapping("/{projectId}")
    public ProjectResponse updateProject(@PathVariable Long projectId,
                                         @AuthenticationPrincipal UserDetails user,
                                         @RequestBody ProjectRequest request) {
        return ProjectResponse.from(
                projectService.updateProject(projectId, user.getUsername(), request.projectName()));
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Map<String, String>> deleteProject(@PathVariable Long projectId,
                                                             @AuthenticationPrincipal UserDetails user) {
        projectService.deleteProject(projectId, user.getUsername());
        return ResponseEntity.ok(Map.of("message", "Project deleted successfully."));
    }

    @GetMapping("/{projectId}/members")
    public List<MemberResponse> getMembers(@PathVariable Long projectId,
                                           @AuthenticationPrincipal UserDetails user) {
        return projectService.findMembers(projectId, user.getUsername()).stream()
                .map(MemberResponse::from)
                .toList();
    }

    @PostMapping("/{projectId}/members")
    public ResponseEntity<MemberResponse> addMember(@PathVariable Long projectId,
                                                    @AuthenticationPrincipal UserDetails user,
                                                    @RequestBody MemberRequest request) {
        Member member = projectService.addMember(projectId, user.getUsername(), request.username());
        return ResponseEntity.status(201).body(MemberResponse.from(member));
    }

    public record ProjectRequest(String projectName) {
    }

    public record MemberRequest(String username) {
    }

    public record MemberResponse(Long userId, String username, String role, String joinedAt) {
        static MemberResponse from(Member member) {
            return new MemberResponse(
                    member.getUser().getUserId(),
                    member.getUser().getUsername(),
                    member.getRole(),
                    member.getJoinedAt().toString());
        }
    }

    public record ProjectResponse(Long projectId, String projectName, String inviteCode,
                                  String createdAt, String createdBy, String role,
                                  boolean admin) {
        static ProjectResponse from(ProjectEntity project) {
            return new ProjectResponse(
                    project.getProjectId(),
                    project.getProjectName(),
                    project.getInviteCode(),
                    project.getCreatedAt().toString(),
                    project.getCreatedBy().getUsername(),
                    "ADMIN",
                    true);
        }

        static ProjectResponse from(Member member) {
            ProjectEntity project = member.getProject();
            boolean admin = "ADMIN".equalsIgnoreCase(member.getRole());
            return new ProjectResponse(
                    project.getProjectId(),
                    project.getProjectName(),
                    project.getInviteCode(),
                    project.getCreatedAt().toString(),
                    project.getCreatedBy().getUsername(),
                    member.getRole(),
                    admin);
        }
    }
}
