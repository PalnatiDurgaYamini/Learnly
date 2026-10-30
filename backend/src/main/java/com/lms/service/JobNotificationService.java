package com.lms.service;

import com.lms.model.BackgroundJob;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/** Pushes every job status change to the Admin dashboard over WebSocket (/topic/jobs). */
@Service @RequiredArgsConstructor
public class JobNotificationService {
    private final SimpMessagingTemplate messaging;

    public void publish(BackgroundJob job) {
        messaging.convertAndSend("/topic/jobs", job);
    }
}
