package poly.edu.quanlynhahang.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AuthTokenFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);
            // Pre-validate: JWT must have exactly 2 dots (header.payload.signature)
            if (jwt != null && jwt.chars().filter(ch -> ch == '.').count() == 2 && jwtUtils.validateJwtToken(jwt)) {
                String username = jwtUtils.getUserNameFromJwtToken(jwt);

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if (!(userDetails instanceof CustomUserDetails customUserDetails)
                        || !customUserDetails.isEnabled()
                        || customUserDetails.getTokenVersion() != jwtUtils.getTokenVersionFromJwtToken(jwt)) {
                    filterChain.doFilter(request, response);
                    return;
                }
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                if (customUserDetails.isPasswordChangeRequired()
                        && !("PUT".equals(request.getMethod())
                        && "/api/auth/password".equals(request.getRequestURI()))) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"status\":403,\"code\":\"PASSWORD_CHANGE_REQUIRED\","
                            + "\"message\":\"Bạn phải đổi mật khẩu trước khi tiếp tục.\"}");
                    return;
                }
            }
        } catch (Exception e) {
            System.err.println("Cannot set user authentication: " + e);
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7); // Lấy chuỗi sau chữ "Bearer "
        }
        return null;
    }
}
