package com.backend.kdt.auth.controller;

import com.backend.kdt.auth.dto.ApiResponse;
import com.backend.kdt.auth.dto.KakaoDTO;
import com.backend.kdt.auth.dto.OAuthFinalRegisterRequest;
import com.backend.kdt.auth.dto.LoginResponse;
import com.backend.kdt.auth.entity.User;
import com.backend.kdt.auth.service.KakaoService;
import com.backend.kdt.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/kakao")
@Slf4j
public class KakaoController {

    private final KakaoService kakaoService;
    private final UserService userService;

    @GetMapping("/callback")
    public String handleKakaoCallback(
            @RequestParam("code") String code) {
        return "redirect:/kakao-join?code=" + code;
    }

    @GetMapping("/login-url")
    public ResponseEntity<ApiResponse<String>> getKakaoLoginUrl() {
        String loginUrl = kakaoService.getKakaoLoginUrl();
        return ResponseEntity.ok(ApiResponse.onSuccess(loginUrl));
    }

    @PostMapping("/final-register")
    @Operation(summary = "카카오 최종 회원가입 및 로그인", description = "카카오 인증 후 회원가입을 완료하고 JWT 발급")
    public ResponseEntity<ApiResponse<LoginResponse>> kakaoFinalRegister(
            @RequestBody OAuthFinalRegisterRequest request,
            HttpServletResponse response) {

        KakaoDTO kakaoDTO = kakaoService.getKakaoUserInfo(request.getCode());

        kakaoService.registerKakaoUser(
                kakaoDTO.getAccountEmail(),
                kakaoDTO.getName(),
                kakaoDTO.getProfileImageUrl(),
                request.getGender(),
                request.getBirthYear()
        );

        User newUser = userService.getUserByEmail(kakaoDTO.getAccountEmail());
        userService.setLoginCookie(response, newUser.getEmail());

        userService.authenticateUser(newUser);

        return ResponseEntity.ok(ApiResponse.onSuccess(userService.loginResponse(newUser)));
    }

    @PostMapping("/login")
    @Operation(summary = "카카오 로그인", description = "카카오 계정으로 로그인합니다.")
    public ResponseEntity<LoginResponse> kakaoLogin(@RequestParam("email") String email, HttpServletResponse response) {
        User user = userService.getUserByEmailOrNull(email);

        if (user == null) {
            throw new IllegalArgumentException("등록되지 않은 이메일입니다. 회원가입을 먼저 진행해주세요.");
        }

        userService.setLoginCookie(response, user.getEmail());
        return ResponseEntity.ok(userService.loginResponse(user));
    }

    @Operation(summary = "로그아웃", description = "현재 로그인한 사용자의 계정을 로그아웃합니다.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logoutUsers(HttpServletResponse response) {
        Long userId = userService.getAuthenticatedUserId();
        userService.logoutUser(userId, response);

        return ResponseEntity.ok(ApiResponse.onSuccess("로그아웃 성공"));
    }

    @Operation(summary = "회원 탈퇴", description = "현재 로그인한 사용자의 계정을 탈퇴합니다.")
    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<String>> deleteUsers(@RequestParam String email) {
        userService.deleteUsers(email);

        return new ResponseEntity<>(ApiResponse.onSuccess("회원 탈퇴 처리가 완료되었습니다."), HttpStatus.OK);
    }
}
