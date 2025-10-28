package com.zeebra.domain.notification.dto;

import com.zeebra.domain.notification.entity.Notification;

public record NotificationResponse (
    Notification notification
)
{

}
