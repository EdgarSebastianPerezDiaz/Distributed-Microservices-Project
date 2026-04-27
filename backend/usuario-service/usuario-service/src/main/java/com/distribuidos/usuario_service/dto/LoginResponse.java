package com.distribuidos.usuario_service.dto;

public class LoginResponse {
    private String token;
    private String type;
    private UserResponse user;
    
    public LoginResponse() {}
    
    public LoginResponse(String token, String type, UserResponse user) {
        this.token = token;
        this.type = type;
        this.user = user;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public UserResponse getUser() { return user; }
    public void setUser(UserResponse user) { this.user = user; }
    
    public static class Builder {
        private String token;
        private String type;
        private UserResponse user;
        
        public Builder token(String token) { this.token = token; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder user(UserResponse user) { this.user = user; return this; }
        public LoginResponse build() { return new LoginResponse(token, type, user); }
    }
}