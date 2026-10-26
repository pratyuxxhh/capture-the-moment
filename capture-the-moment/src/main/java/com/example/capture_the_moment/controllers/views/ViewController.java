package com.example.capture_the_moment.controllers.views;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;

@Controller
public class ViewController {

    @GetMapping
    public String view(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                ? "redirect:/dashboard"
                : "redirect:/signin";
    }

    @GetMapping("/signin")
    public String signin() {
        return "signin";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/settings")
    public String settings() {
        return "settings";
    }

    @GetMapping("/projects/{projectId}/settings")
    public String projectSettings() {
        return "project-settings";
    }

    @GetMapping("/projects/{projectId}/audios")
    public String audios(@PathVariable Long projectId, Model model) {
        model.addAttribute("projectId", projectId);
        return "project-audios";
    }

    @GetMapping("/projects/{projectId}/transcription")
    public String transcription(@PathVariable Long projectId, Model model) {
        model.addAttribute("projectId", projectId);
        return "project-transcription";
    }

    @GetMapping("/projects/{projectId}/transcription/{audioId}")
    public String transcriptionDetail(@PathVariable Long projectId,
                                      @PathVariable Long audioId,
                                      Model model) {
        model.addAttribute("projectId", projectId);
        model.addAttribute("audioId", audioId);
        return "transcription-detail";
    }

    @GetMapping("/projects/{projectId}/kanban")
    public String kanban(@PathVariable Long projectId, Model model) {
        model.addAttribute("projectId", projectId);
        return "project-kanban";
    }

    @GetMapping("/projects/{projectId}/chit-chat")
    public String chitChat(@PathVariable Long projectId, Model model) {
        model.addAttribute("projectId", projectId);
        return "project-chit-chat";
    }

    @GetMapping("/projects/{projectId}/members")
    public String members(@PathVariable Long projectId, Model model) {
        model.addAttribute("projectId", projectId);
        return "project-members";
    }
}
