package poly.edu.quanlynhahang.dto;

public class LoginRequest {
    private String username;
    private String password;

    // Tự sinh Getter và Setter để không phụ thuộc vào Lombok
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}