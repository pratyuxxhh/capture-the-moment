package com.example.capture_the_moment.services.chat;

import com.example.capture_the_moment.entity.chat.Message;
import com.example.capture_the_moment.entity.chat.Room;
import com.example.capture_the_moment.entity.user.UserEntity;
import com.example.capture_the_moment.repository.chat.ChatRepository;
import com.example.capture_the_moment.repository.project.MemberRepository;
import com.example.capture_the_moment.repository.project.ProjectRepository;
import com.example.capture_the_moment.repository.chat.RoomRepository;
import com.example.capture_the_moment.repository.user.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class ChatService {
    public static final int MAX_MESSAGE_LENGTH = 2_000;

    private final ChatRepository chatRepository;
    private final RoomRepository roomRepository;
    private final ProjectRepository projectRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;

    public ChatService(ChatRepository chatRepository, RoomRepository roomRepository,
                       ProjectRepository projectRepository, MemberRepository memberRepository,
                       UserRepository userRepository) {
        this.chatRepository = chatRepository;
        this.roomRepository = roomRepository;
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ChatHistory history(Long projectId, String username) {
        requireMembership(projectId, username);
        Room room = getOrCreateRoom(projectId);
        List<ChatMessage> messages = chatRepository
                .findTop100ByRoomRoomIdOrderBySentAtDescMessageIdDesc(room.getRoomId())
                .stream()
                .map(ChatMessage::from)
                .sorted(Comparator.comparing(ChatMessage::sentAt).thenComparing(ChatMessage::messageId))
                .toList();
        return new ChatHistory(messages);
    }

    @Transactional
    public ChatMessage saveMessage(Long projectId, String username, String content) {
        requireMembership(projectId, username);
        String normalized = content == null ? "" : content.trim();
        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message cannot be empty.");
        }
        if (normalized.length() > MAX_MESSAGE_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Message must be 2,000 characters or fewer.");
        }
        UserEntity sender = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Authenticated user was not found."));
        Message message = new Message();
        message.setRoom(getOrCreateRoom(projectId));
        message.setSender(sender);
        message.setContent(normalized);
        message.setSentAt(LocalDateTime.now());
        return ChatMessage.from(chatRepository.save(message));
    }

    @Transactional
    public void deleteProjectChat(Long projectId) {
        chatRepository.deleteAllByRoomProjectProjectId(projectId);
        roomRepository.deleteByProjectProjectId(projectId);
    }

    @Transactional
    public void requireMembership(Long projectId, String username) {
        if (projectId == null || projectRepository.findById(projectId).isEmpty()
                || memberRepository.findByProjectProjectIdAndUserUsername(projectId, username).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not a member of this project.");
        }
    }

    private Room getOrCreateRoom(Long projectId) {
        return roomRepository.findByProjectProjectId(projectId).orElseGet(() -> {
            Room room = new Room();
            room.setProject(projectRepository.getReferenceById(projectId));
            room.setCreatedAt(LocalDateTime.now());
            return roomRepository.save(room);
        });
    }

    public record ChatHistory(List<ChatMessage> messages) {
    }

    public record ChatMessage(Long messageId, String username, String content, String sentAt) {
        static ChatMessage from(Message message) {
            return new ChatMessage(message.getMessageId(), message.getSender().getUsername(),
                    message.getContent(), message.getSentAt().toString());
        }
    }
}