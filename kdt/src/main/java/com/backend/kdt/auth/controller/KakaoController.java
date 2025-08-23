package com.backend.kdt.auth.controller;

import com.backend.kdt.auth.dto.ApiResponse;
import com.backend.kdt.auth.dto.LoginResponseDto;
import com.backend.kdt.auth.service.KakaoService;
import com.backend.kdt.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
    @Operation(summary = "카카오 로그인 URL 생성", description = "카카오 OAuth 인증을 위한 URL을 생성합니다.")
    public ResponseEntity<ApiResponse<String>> getKakaoLoginUrl() {
        String loginUrl = kakaoService.getKakaoLoginUrl();
        return ResponseEntity.ok(ApiResponse.onSuccess(loginUrl));
    }

//    @PostMapping("/login")
//    @Operation(summary = "카카오 로그인/회원가입", description = "카카오 계정으로 로그인하거나 자동 회원가입합니다.")
//    public ResponseEntity<ApiResponse<LoginResponse>> kakaoLogin(
//            @Valid @RequestBody KakaoRegisterRequest request,
//            HttpServletResponse response) {
//
//        try {
//            LoginResponse loginResponse = kakaoService.processKakaoLogin(request.getCode());
//
//            // JWT 쿠키 설정
//            userService.setLoginCookie(response, loginResponse.getEmail());
//
//            return ResponseEntity.ok(ApiResponse.onSuccess(loginResponse));
//
//        } catch (IllegalStateException e) {
//            // 중복 이메일 등의 상태 오류
//            log.warn("Kakao login failed - State error: {}", e.getMessage());
//            return ResponseEntity.status(HttpStatus.CONFLICT)
//                    .body(ApiResponse.onFailure("STATE_ERROR", e.getMessage()));
//        } catch (IllegalArgumentException e) {
//            // 잘못된 코드 등의 입력 오류
//            log.warn("Kakao login failed - Invalid argument: {}", e.getMessage());
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                    .body(ApiResponse.onFailure("INVALID_REQUEST", e.getMessage()));
//        } catch (Exception e) {
//            log.error("Unexpected error during kakao login", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(ApiResponse.onFailure("LOGIN_FAILED", "카카오 로그인 처리 중 오류가 발생했습니다."));
//        }
//    }

    @PostMapping("/loginm")
    @Operation(summary = "카카오 로그인/회원가입", description = "카카오 계정으로 로그인하거나 자동 회원가입합니다.")
    public ResponseEntity<ApiResponse<LoginResponseDto>> kakaoLoginWithParam(
            @RequestParam("code") String code,
            HttpServletResponse response) {

        try {
            LoginResponseDto loginResponseDto = kakaoService.processKakaoLogin(code);

            // JWT 쿠키 설정
            userService.setLoginCookie(response, loginResponseDto.getEmail());

            return ResponseEntity.ok(ApiResponse.onSuccess(loginResponseDto));

        } catch (IllegalStateException e) {
            log.warn("Kakao login failed - State error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.onFailure("STATE_ERROR", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("Kakao login failed - Invalid argument: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.onFailure("INVALID_REQUEST", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during kakao login", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.onFailure("LOGIN_FAILED", "카카오 로그인 처리 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "현재 로그인한 사용자의 계정을 로그아웃합니다.")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletResponse response) {
        try {
            Long userId = userService.getAuthenticatedUserId();
            userService.logoutUser(userId, response);
            return ResponseEntity.ok(ApiResponse.onSuccess("로그아웃 성공"));
        } catch (Exception e) {
            log.error("Logout failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.onFailure("LOGOUT_FAILED", "로그아웃 처리 중 오류가 발생했습니다."));
        }
    }

    @DeleteMapping("/delete")
    @Operation(summary = "회원 탈퇴", description = "현재 로그인 중인 사용자 계정을 삭제하고 쿠키를 정리합니다.")
    public ResponseEntity<ApiResponse<String>> forceDeleteUser(HttpServletResponse response) {
        try {
            // 현재 로그인된 사용자 정보 가져오기 (JWT에서)
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            String email = null;
            if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
                email = ((UserDetails) authentication.getPrincipal()).getUsername();
            } else if (authentication != null && authentication.getName() != null &&
                    !authentication.getName().equals("anonymousUser")) {
                email = authentication.getName();
            }

            if (email == null || email.isEmpty() || email.equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.onFailure("NO_LOGIN_INFO", "로그인 정보를 찾을 수 없습니다."));
            }

            // 사용자 삭제
            userService.deleteUser(email);

            // JWT 쿠키 삭제
            userService.clearAccessTokenCookie(response);

            return ResponseEntity.ok(ApiResponse.onSuccess("회원 탈퇴 및 쿠키 정리가 완료되었습니다. (삭제된 계정: " + email + ")"));

        } catch (Exception e) {
            log.error("Force user deletion failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.onFailure("DELETE_FAILED", "회원 탈퇴 처리 중 오류가 발생했습니다."));
        }
    }

//    @DeleteMapping("/delete-by-email")
//    @Operation(summary = "회원 탈퇴 (이메일)", description = "이메일로 사용자 계정을 탈퇴합니다.")
//    public ResponseEntity<ApiResponse<String>> deleteUserByEmail(@RequestParam String email) {
//        try {
//            userService.deleteUser(email);
//            return ResponseEntity.ok(ApiResponse.onSuccess("회원 탈퇴 처리가 완료되었습니다."));
//        } catch (Exception e) {
//            log.error("User deletion by email failed", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(ApiResponse.onFailure("DELETE_FAILED", "회원 탈퇴 처리 중 오류가 발생했습니다."));
//        }
//    }
}