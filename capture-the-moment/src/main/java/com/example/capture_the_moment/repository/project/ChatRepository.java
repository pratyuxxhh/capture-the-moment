package com.example.capture_the_moment.repository.project;

import com.example.capture_the_moment.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRepository extends JpaRepository<Message, Long> {
    List<Message> findTop100ByRoomRoomIdOrderBySentAtDescMessageIdDesc(Long roomId);

    void deleteAllByRoomProjectProjectId(Long projectId);
}
