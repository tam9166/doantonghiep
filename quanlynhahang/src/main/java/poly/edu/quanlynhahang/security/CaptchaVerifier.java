package poly.edu.quanlynhahang.security;

public interface CaptchaVerifier {
    boolean verify(String token, String clientIp, String action);
}
