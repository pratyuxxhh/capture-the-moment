package com.example.capture_the_moment.services.project;

import com.example.capture_the_moment.entity.Member;
import com.example.capture_the_moment.entity.MemberId;
import com.example.capture_the_moment.entity.ProjectEntity;
import com.example.capture_the_moment.entity.UserEntity;
import com.example.capture_the_moment.repository.project.MemberRepository;
import com.example.capture_the_moment.repository.project.ProjectRepository;
import com.example.capture_the_moment.repository.user.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {
    private static final String ADMIN_ROLE = "ADMIN";

    private final ProjectRepository projectRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final InviteService inviteService;

    public ProjectService(ProjectRepository projectRepository,
                          MemberRepository memberRepository,
                          UserRepository userRepository,
                          InviteService inviteService) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.inviteService = inviteService;
    }

    @Transactional
    public List<Member> findMembershipsForUser(String username) {
        return memberRepository.findByUserUsernameOrderByJoinedAtDesc(username);
    }

    @Transactional
    public ProjectEntity createProject(String username, String projectName) {
        String normalizedName = normalizeProjectName(projectName);
        UserEntity user = findUser(username);
        LocalDateTime now = LocalDateTime.now();

        ProjectEntity project = new ProjectEntity();
        project.setProjectName(normalizedName);
        project.setInviteCode(UUID.randomUUID().toString());
        project.setCreatedBy(user);
        project.setCreatedAt(now);
        ProjectEntity savedProject = projectRepository.save(project);

        Member member = new Member();
        MemberId memberId = new MemberId();
        memberId.setUserId(user.getUserId());
        memberId.setProjectId(savedProject.getProjectId());
        member.setId(memberId);
        member.setUser(user);
        member.setProject(savedProject);
        member.setRole(ADMIN_ROLE);
        member.setJoinedAt(now);
        memberRepository.save(member);
        return savedProject;
    }

    @Transactional
    public ProjectEntity updateProject(Long projectId, String username, String projectName) {
        Member member = requireAdmin(projectId, username);
        ProjectEntity project = member.getProject();
        project.setProjectName(normalizeProjectName(projectName));
        return projectRepository.save(project);
    }

    @Transactional
    public List<Member> findMembers(Long projectId, String username) {
        requireMembership(projectId, username);
        return memberRepository.findByProjectProjectIdOrderByJoinedAtAsc(projectId);
    }

    @Transactional
    public Member addMember(Long projectId, String username, String memberUsername) {
        return inviteService.inviteMember(projectId, username, memberUsername);
    }

    @Transactional
    public void deleteProject(Long projectId, String username) {
        requireAdmin(projectId, username);
        memberRepository.deleteAllByProjectProjectId(projectId);
        projectRepository.deleteById(projectId);
    }

    private Member requireAdmin(Long projectId, String username) {
        Member member = requireMembership(projectId, username);
        if (!ADMIN_ROLE.equalsIgnoreCase(member.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only project administrators can change project settings.");
        }
        return member;
    }

    private Member requireMembership(Long projectId, String username) {
        return memberRepository.findByProjectProjectIdAndUserUsername(projectId, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You are not a member of this project."));
    }

    private UserEntity findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Authenticated user was not found."));
    }

    private String normalizeProjectName(String projectName) {
        if (projectName == null || projectName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project name is required.");
        }
        String normalizedName = projectName.trim();
        if (normalizedName.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Project name must be 100 characters or fewer.");
        }
        return normalizedName;
    }
}
