package com.vishnu.finance_tracker.model;

public class ApiResponse<T> {

    private boolean success;
    private T data;

    public ApiResponse(boolean success, T data) {
        this.success = success;
        this.data = data;
    }

    public boolean isSuccess() { return success; }
    public T getData() { return data; }
}
