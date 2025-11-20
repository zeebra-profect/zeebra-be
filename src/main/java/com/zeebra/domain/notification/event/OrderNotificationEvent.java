package com.zeebra.domain.notification.event;

import com.zeebra.domain.notification.entity.NotificationType;
import com.zeebra.domain.order.entity.Order;

public class OrderNotificationEvent extends NotificationEvent {

    private Order order;

    public OrderNotificationEvent(Long memberId, NotificationType type, String displayText, String imgUrl, Order order) {
        super(memberId, type, displayText, imgUrl);
        this.order = order;
    }
}
