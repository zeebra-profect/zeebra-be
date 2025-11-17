package com.zeebra.global;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {
    private String status;    // "success" | "error"
    private String message; //nullable
    private T data;        //nullable
    private LocalDateTime sendTime;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status("success")
                .data(data)
                .sendTime(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> successMessage(String message) {
        return ApiResponse.<T>builder()
                .status("success")
                .message(message)
                .sendTime(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(T data, String message) {
        return ApiResponse.<T>builder()
                .status("error")
                .data(data)
                .message(message)
                .sendTime(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> errorMessage(String message) {
        return ApiResponse.<T>builder()
                .status("error")
                .message(message)
                .sendTime(LocalDateTime.now())
                .build();
    }

}