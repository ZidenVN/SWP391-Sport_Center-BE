package com.fptu.swp391.sportscentermanager.security;

import com.fptu.swp391.sportscentermanager.entity.Account;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
public class CustomUserDetails implements UserDetails {
    private Account account;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String roleName = (account.getRole() != null) ? account.getRole().getRoleName() : "USER";
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_" + roleName));
    }

    @Override
    public String getPassword(){
        return account.getPasswordHash();
    }

    @Override
    public String getUsername(){
        return account.getUsername();
    }

    @Override
    public boolean isAccountNonExpired(){
        return true;
    }

    @Override
    public boolean isAccountNonLocked(){
        return "ACTIVE".equalsIgnoreCase(account.getStatus());
    }

    @Override
    public boolean isCredentialsNonExpired(){
        return true;
    }

    @Override
    public boolean isEnabled(){
        return true;
    }
}
