package com.khata4u.backend.dto;

public class RegisterResponse {
    private Long id;
    private String message;
    public RegisterResponse() {}
    public RegisterResponse(Long id, String message) { this.id = id; this.message = message; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

