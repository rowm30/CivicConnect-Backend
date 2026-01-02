package com.civicconnect.api.service;

import com.civicconnect.api.dto.analytics.AnalyticsDTOs.*;
import com.civicconnect.api.entity.analytics.ActivityLog;
import com.civicconnect.api.entity.analytics.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyUserOnline(AppUser user) {
        UserStatusUpdate update = UserStatusUpdate.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .photoUrl(user.getPhotoUrl())
                .isActive(true)
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/user-status", update);
        log.debug("Notified: User {} came online", user.getEmail());
    }

    public void notifyUserOffline(AppUser user) {
        UserStatusUpdate update = UserStatusUpdate.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .photoUrl(user.getPhotoUrl())
                .isActive(false)
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/user-status", update);
        log.debug("Notified: User {} went offline", user.getEmail());
    }

    public void notifyNewActivity(AppUser user, ActivityLog.ActivityType type, String activityName) {
        ActivityUpdate update = ActivityUpdate.builder()
                .userId(user.getId())
                .userName(user.getName())
                .userPhotoUrl(user.getPhotoUrl())
                .activityType(type.name())
                .activityName(activityName)
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/activities", update);
    }

    public void broadcastActiveUserCount(long count) {
        messagingTemplate.convertAndSend("/topic/active-count", count);
    }
}
