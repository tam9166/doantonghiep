package poly.edu.quanlynhahang.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.Authority;

public class CustomUserDetails implements UserDetails {

    private Account account;

    public CustomUserDetails(Account account) {
        this.account = account;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        
        // 1. Lấy quyền từ Database (nếu SQL Server có dữ liệu)
        if (account.getAuthorities() != null) {
            for (Authority auth : account.getAuthorities()) {
                String roleName = auth.getRole().getName();
                if (!roleName.startsWith("ROLE_")) {
                    roleName = "ROLE_" + roleName;
                }
                authorities.add(new SimpleGrantedAuthority(roleName));
            }
        }

        return authorities;
    }

    @Override
    public String getPassword() { return account.getPassword(); }
    @Override
    public String getUsername() { return account.getUsername(); }
    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return !Boolean.FALSE.equals(account.getEnabled()); }
    
    public Account getAccount() { return account; }
    public long getTokenVersion() { return account.getTokenVersion() == null ? 0L : account.getTokenVersion(); }
}
