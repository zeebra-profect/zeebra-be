package com.zeebra.domain.notification.service;

import com.zeebra.domain.notification.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("ㅇㅅㅇ")
    public void createNotification_validInput_success() {

    }
}
