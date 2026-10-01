package com.hotel.system.dto.auth;
import lombok.AllArgsConstructor; import lombok.Getter; import lombok.Setter;

@Getter @Setter @AllArgsConstructor
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private String id;
    private String email;
    private String fullName;
    private String role;

    public JwtResponse(String token, String id, String email, String fullName, String role) {
        this.token = token;
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }
}