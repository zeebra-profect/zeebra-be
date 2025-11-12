package com.zeebra.domain.notification.component;

import com.zeebra.domain.notification.entity.NotificationType;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Component
public class NotificationUrlFactory {
    public String createUrl(NotificationType type, Object object) {
        if (object == null)
            return null;

        switch (type) {
            case TEST_OBJECT:
                return "/dummy";
            case ORDER_CONFIRMED:
                return extractUrl(object, "orderId", "/orders/", "mypage/orderhistory");
            case ORDER_SHIPPED:
                return extractUrl(object, "orderId", "/orders/", "mypage/orderhistory");
            case ORDER_DELIVERED:
                return extractUrl(object, "orderId", "/orders/", "mypage/orderhistory");
            default:
                return null;
        }
    }

    private String extractUrl(Object object, String fieldName, String urlPrefix, String fallbackUrl) {
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(object);
            return urlPrefix + value;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return fallbackUrl;
        }
    }
}
