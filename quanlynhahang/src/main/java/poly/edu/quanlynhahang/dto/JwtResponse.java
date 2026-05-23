package poly.edu.quanlynhahang.dto;

import java.util.List;

import lombok.Data;

@Data
public class JwtResponse {
    private String token;
    private String username;
    private List<String> roles;

    public JwtResponse(String token, String username, List<String> roles) {
        this.token = token;
        this.username = username;
        this.roles = roles;
    }
}