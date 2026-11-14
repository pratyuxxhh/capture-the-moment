package com.example.capture_the_moment.services.project;

import com.example.capture_the_moment.entity.project.Member;
import com.example.capture_the_moment.entity.project.MemberId;
import com.example.capture_the_moment.entity.user.UserEntity;
import com.example.capture_the_moment.repository.project.MemberRepository;
import com.example.capture_the_moment.repository.project.ProjectRepository;
import com.example.capture_the_moment.repository.user.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class InviteService {
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String MEMBER_ROLE = "MEMBER";

    private final MemberRepository memberRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public InviteService(MemberRepository memberRepository,
                         ProjectRepository projectRepository,
                         UserRepository userRepository) {
        this.memberRepository = memberRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Member inviteMember(Long projectId, String inviterUsername, String invitedUsername) {
        Member inviter = memberRepository
                .findByProjectProjectIdAndUserUsername(projectId, inviterUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You are not a member of this project."));
        if (!ADMIN_ROLE.equalsIgnoreCase(inviter.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only project administrators can add members.");
        }

        if (invitedUsername == null || invitedUsername.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Member username is required.");
        }
        UserEntity invitedUser = userRepository.findByUsername(invitedUsername.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No account exists for that username."));
        if (memberRepository.findByProjectProjectIdAndUserUsername(projectId, invitedUser.getUsername())
                .isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "That user is already a member of this project.");
        }

        Member member = new Member();
        MemberId memberId = new MemberId();
        memberId.setUserId(invitedUser.getUserId());
        memberId.setProjectId(projectId);
        member.setId(memberId);
        member.setUser(invitedUser);
        member.setProject(projectRepository.getReferenceById(projectId));
        member.setRole(MEMBER_ROLE);
        member.setJoinedAt(LocalDateTime.now());
        return memberRepository.save(member);
    }
}
