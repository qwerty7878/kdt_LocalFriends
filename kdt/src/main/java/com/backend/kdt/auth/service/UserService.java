package com.backend.kdt.auth.service;

import com.backend.kdt.auth.dto.LoginResponse;
import com.backend.kdt.auth.entity.User;
import com.backend.kdt.auth.repository.UserRepository;
import com.backend.kdt.auth.security.CustomUserDetails;
import com.backend.kdt.auth.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email: " + email));
    }

    public User getUserByEmailOrNull(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public void setLoginCookie(HttpServletResponse response, String email) {
        jwtService.addAccessTokenCookie(response, email); // 내부적으로 role은 고정되었거나 기본값일 수 있음
    }

    public LoginResponse loginResponse(User user) {
        return LoginResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .profile(user.getProfile())
                .birthYear(user.getBirthYear())
                .platform(user.getPlatform())
                .build();
    }

    @Transactional
    public void logoutUser(Long userId, HttpServletResponse response) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        jwtService.clearAccessTokenCookie(response);
    }

    @Transactional
    public void deleteUsers(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다"));
        userRepository.delete(user);
    }

    public void authenticateUser(User user) {
        UserDetails userDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    public Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            String email = ((UserDetails) authentication.getPrincipal()).getUsername();
            return getUserByEmail(email).getId();
        }
        throw new IllegalArgumentException("User is not authenticated.");
    }
}