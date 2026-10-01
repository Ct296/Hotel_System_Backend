package com.hotel.system.security;
import com.hotel.system.entity.Account;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.Collections;

public class UserDetailsImpl implements UserDetails {
    private String id;
    private String email;
    private String password;
    private String fullName;
    private Collection<? extends GrantedAuthority> authorities;

    public UserDetailsImpl(String id, String email, String password, String fullName, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.authorities = authorities;
    }

    public static UserDetailsImpl build(Account account) {
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + account.getUser().getRole().name());
        String fullName = account.getUser().getLastName() + " " + account.getUser().getFirstName();
        return new UserDetailsImpl(
                account.getId(),
                account.getUser().getEmail(),
                account.getPassword(),
                fullName,
                Collections.singletonList(authority));
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return email; } // Dùng email làm username
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; } // Bạn có thể map với AccountStatus sau
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
    
    public String getId() { return id; }
    public String getFullName() { return fullName; }
}