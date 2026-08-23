package poly.edu.quanlynhahang.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class ApplicationUploadRequest {
    @NotBlank
    @Size(max = 200)
    private String fullname;

    @NotBlank
    @Size(max = 20)
    @Pattern(regexp = "^(?:\\+84|0)[0-9 .-]{8,14}$")
    private String phone;

    @Email
    @Size(max = 100)
    private String email;

    @Size(max = 50_000)
    private String message;

    @NotNull
    @Positive
    private Integer postId;

    private MultipartFile file;

    public String getFullname() { return fullname; }
    public void setFullname(String fullname) { this.fullname = fullname; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Integer getPostId() { return postId; }
    public void setPostId(Integer postId) { this.postId = postId; }
    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }
}
