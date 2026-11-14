package com.example.capture_the_moment.controllers.kanban;

import com.example.capture_the_moment.entity.kanban.Task;
import com.example.capture_the_moment.services.kanban.KanbanServices;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/project/{projectId}/kanban")
public class KanbanController {
    private final KanbanServices kanbanServices;

    public KanbanController(KanbanServices kanbanServices) {
        this.kanbanServices = kanbanServices;
    }

    @GetMapping("/tasks")
    public List<TaskResponse> tasks(@PathVariable Long projectId,
                                    @AuthenticationPrincipal UserDetails user) {
        return kanbanServices.tasks(projectId, user.getUsername()).stream().map(TaskResponse::from).toList();
    }

    @PostMapping("/tasks")
    public TaskResponse create(@PathVariable Long projectId,
                               @AuthenticationPrincipal UserDetails user,
                               @RequestBody TaskRequest request) {
        return TaskResponse.from(kanbanServices.createTask(projectId, user.getUsername(),
                request.title(), request.description(), request.assigneeUsername()));
    }

    @PatchMapping("/tasks/{taskId}")
    public TaskResponse updateStatus(@PathVariable Long projectId, @PathVariable Long taskId,
                                     @AuthenticationPrincipal UserDetails user,
                                     @RequestBody StatusRequest request) {
        return TaskResponse.from(kanbanServices.updateStatus(projectId, taskId, user.getUsername(), request.status()));
    }

    @DeleteMapping("/tasks/{taskId}")
    public void delete(@PathVariable Long projectId, @PathVariable Long taskId,
                       @AuthenticationPrincipal UserDetails user) {
        kanbanServices.deleteTask(projectId, taskId, user.getUsername());
    }

    public record TaskRequest(String title, String description, String assigneeUsername) {}
    public record StatusRequest(String status) {}
    public record TaskResponse(Long taskId, String title, String description, String status,
                               String assigneeUsername, String assigneeAvatar, String updatedAt) {
        public static TaskResponse from(Task task) {
            return new TaskResponse(task.getTaskId(), task.getTitle(), task.getDescription(), task.getStatus(),
                    task.getAssignee() == null ? null : task.getAssignee().getUsername(),
                    task.getAssignee() == null ? null : task.getAssignee().getAvatar(),
                    task.getUpdatedAt().toString());
        }
    }
}
