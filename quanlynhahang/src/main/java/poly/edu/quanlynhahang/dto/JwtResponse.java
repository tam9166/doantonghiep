package poly.edu.quanlynhahang.dto;

import java.util.List;

import lombok.Data;

@Data
public class JwtResponse {
    private String token;
    private String username;
    private List<String> roles;
    private String assignedArea;
    private String shift;

    public JwtResponse(String token, String username, List<String> roles, String assignedArea, String shift) {
        this.token = token;
        this.username = username;
        this.roles = roles;
        this.assignedArea = assignedArea;
        this.shift = shift;
    }
}