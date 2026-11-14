package com.example.capture_the_moment.services.kanban;

import com.example.capture_the_moment.entity.kanban.Kanban;
import com.example.capture_the_moment.entity.project.Member;
import com.example.capture_the_moment.entity.kanban.Task;
import com.example.capture_the_moment.entity.user.UserEntity;
import com.example.capture_the_moment.repository.kanban.KanbanRepository;
import com.example.capture_the_moment.repository.project.MemberRepository;
import com.example.capture_the_moment.repository.project.ProjectRepository;
import com.example.capture_the_moment.repository.kanban.TaskRepository;
import com.example.capture_the_moment.repository.user.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class KanbanServices {
    public static final String TODO = "TODO";
    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String DONE = "DONE";

    private final KanbanRepository kanbanRepository;
    private final MemberRepository memberRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public KanbanServices(KanbanRepository kanbanRepository,
                          MemberRepository memberRepository,
                          ProjectRepository projectRepository,
                          TaskRepository taskRepository,
                          UserRepository userRepository) {
        this.kanbanRepository = kanbanRepository;
        this.memberRepository = memberRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Kanban ensureBoard(Long projectId) {
        return kanbanRepository.findByProjectProjectId(projectId).orElseGet(() -> {
            Kanban kanban = new Kanban();
            kanban.setProject(projectRepository.findById(projectId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found.")));
            kanban.setCreatedAt(LocalDateTime.now());
            return kanbanRepository.save(kanban);
        });
    }

    @Transactional
    public List<Task> tasks(Long projectId, String username) {
        requireMembership(projectId, username);
        return new ArrayList<>(ensureBoard(projectId).getTasks());
    }

    @Transactional
    public Task createTask(Long projectId, String username, String title, String description, String assigneeUsername) {
        requireMembership(projectId, username);
        if (title == null || title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task title is required.");
        }
        Kanban kanban = ensureBoard(projectId);
        Task task = new Task();
        task.setKanban(kanban);
        task.setTitle(title.trim());
        task.setDescription(description == null ? "" : description.trim());
        task.setStatus(TODO);
        task.setAssignee(resolveAssignee(assigneeUsername));
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        kanban.getTasks().add(task);
        return taskRepository.save(task);
    }

    @Transactional
    public Task updateStatus(Long projectId, Long taskId, String username, String status) {
        requireMembership(projectId, username);
        if (!List.of(TODO, IN_PROGRESS, DONE).contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid task status.");
        }
        Task task = findTask(projectId, taskId);
        task.setStatus(status);
        task.setUpdatedAt(LocalDateTime.now());
        return task;
    }

    @Transactional
    public void deleteTask(Long projectId, Long taskId, String username) {
        requireMembership(projectId, username);
        Task task = findTask(projectId, taskId);
        task.getKanban().getTasks().remove(task);
        taskRepository.delete(task);
    }

    private Task findTask(Long projectId, Long taskId) {
        return ensureBoard(projectId).getTasks().stream()
                .filter(task -> task.getTaskId().equals(taskId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found."));
    }

    private UserEntity resolveAssignee(String username) {
        return username == null || username.isBlank() ? null : userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignee was not found."));
    }

    public void requireMembership(Long projectId, String username) {
        memberRepository.findByProjectProjectIdAndUserUsername(projectId, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You are not a member of this project."));
    }

    @Transactional
    public List<OnlineUser> onlineUsers(Long projectId, List<String> usernames) {
        return memberRepository.findByProjectProjectIdOrderByJoinedAtAsc(projectId).stream()
                .filter(member -> usernames.contains(member.getUser().getUsername()))
                .map(member -> new OnlineUser(member.getUser().getUsername(), member.getUser().getAvatar()))
                .toList();
    }

    public record OnlineUser(String username, String avatar) {}
}
