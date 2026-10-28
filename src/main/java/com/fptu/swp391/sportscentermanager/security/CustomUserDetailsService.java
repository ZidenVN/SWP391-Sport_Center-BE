package com.fptu.swp391.sportscentermanager.security;

import com.fptu.swp391.sportscentermanager.entity.User;
import com.fptu.swp391.sportscentermanager.repository.RolePermissionRepository;
import com.fptu.swp391.sportscentermanager.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email).orElseThrow(()  -> new UsernameNotFoundException("Không tìm thấy email: " + email));
        List<String> permissionNames = new ArrayList<>();

        if (user.getRole() != null){
            user.getRole().getRoleName(); // nạp Role LAZY trong transaction
            // BE-TODO 6: 1 câu SQL cho toàn bộ quyền thay vì 1 câu cho mỗi quyền
            permissionNames = rolePermissionRepository.findPermissionNamesByRoleId(user.getRole().getRoleId());
        }

        return new CustomUserDetails(user, permissionNames);
    }
}
