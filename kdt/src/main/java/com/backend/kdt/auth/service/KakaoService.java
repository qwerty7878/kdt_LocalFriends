package com.backend.kdt.auth.service;

import com.backend.kdt.auth.dto.KakaoDTO;
import com.backend.kdt.auth.dto.LoginResponseDto;
import com.backend.kdt.auth.entity.User;
import com.backend.kdt.auth.repository.UserRepository;
import com.backend.kdt.auth.security.CustomUserDetails;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.oauth2.sdk.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    @Value("${kakao.client.id}")
    private String kakaoClientId;

    @Value("${kakao.client.secret}")
    private String kakaoClientSecret;

    @Value("${kakao.redirect.url}")
    private String kakaoRedirectUrl;

    private static final String KAKAO_AUTH_URI = "https://kauth.kakao.com";
    private static final String KAKAO_API_URI = "https://kapi.kakao.com";

    public String getKakaoLoginUrl() {
        return String.format("%s/oauth/authorize?client_id=%s&redirect_uri=%s&response_type=code",
                KAKAO_AUTH_URI, kakaoClientId, kakaoRedirectUrl);
    }

    /**
     * 카카오 로그인/회원가입 처리
     */
    @Transactional
    public LoginResponseDto processKakaoLogin(String code) {
        validateAuthorizationCode(code);

        String accessToken = getAccessToken(code);
        KakaoDTO kakaoDTO = getUserInfoWithToken(accessToken);

        // 기존 사용자 확인
        User user = userRepository.findByKakaoId(kakaoDTO.getId())
                .orElseGet(() -> registerNewUser(kakaoDTO));

        // 인증 처리
        authenticateUser(user);

        return LoginResponseDto.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .profile(user.getProfile())
                .build();
    }

    /**
     * 새 사용자 등록
     */
    private User registerNewUser(KakaoDTO kakaoDTO) {
        // 이메일 중복 체크 (선택사항)
        if (kakaoDTO.getAccountEmail() != null &&
                userRepository.existsByEmail(kakaoDTO.getAccountEmail())) {
            throw new IllegalStateException("이미 가입된 이메일입니다.");
        }

        User newUser = User.builder()
                .kakaoId(kakaoDTO.getId())
                .email(kakaoDTO.getAccountEmail())
                .name(kakaoDTO.getName())
                .profile(kakaoDTO.getProfileImageUrl())
                .point(0L)
                .watched(false)
                .build();

        return userRepository.save(newUser);
    }

    private void validateAuthorizationCode(String code) {
        if (StringUtils.isBlank(code)) {
            throw new IllegalArgumentException("Authorization code is required");
        }
    }

    private String getAccessToken(String code) {
        try {
            HttpHeaders headers = createFormHeaders();
            MultiValueMap<String, String> params = createTokenRequestParams(code);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    KAKAO_AUTH_URI + "/oauth/token",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            return parseAccessTokenFromResponse(response.getBody());

        } catch (Exception e) {
            log.error("Failed to get access token from Kakao", e);
            throw new IllegalArgumentException("Failed to get access token", e);
        }
    }

    private HttpHeaders createFormHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return headers;
    }

    private MultiValueMap<String, String> createTokenRequestParams(String code) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoClientId);
        params.add("client_secret", kakaoClientSecret);
        params.add("code", code);
        params.add("redirect_uri", kakaoRedirectUrl);
        return params;
    }

    private String parseAccessTokenFromResponse(String responseBody) {
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            return jsonNode.get("access_token").asText();
        } catch (Exception e) {
            log.error("Failed to parse access token from response", e);
            throw new IllegalArgumentException("Failed to parse token response", e);
        }
    }

    private KakaoDTO getUserInfoWithToken(String accessToken) {
        try {
            HttpHeaders headers = createBearerHeaders(accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    KAKAO_API_URI + "/v2/user/me",
                    HttpMethod.GET,
                    request,
                    String.class
            );

            return parseUserInfoFromResponse(response.getBody());

        } catch (Exception e) {
            log.error("Failed to get user info from Kakao", e);
            throw new IllegalArgumentException("Failed to get user info", e);
        }
    }

    private HttpHeaders createBearerHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return headers;
    }

    private KakaoDTO parseUserInfoFromResponse(String responseBody) {
        try {
            log.info("Kakao user response: {}", responseBody);

            JsonNode jsonNode = objectMapper.readTree(responseBody);
            JsonNode account = jsonNode.get("kakao_account");
            JsonNode profile = account.get("profile");

            long id = jsonNode.get("id").asLong();
            String email = account.has("email") ? account.get("email").asText() : null;
            String nickname = profile.has("nickname") ? profile.get("nickname").asText() : null;
            String profileImageUrl =
                    profile.has("profile_image_url") ? profile.get("profile_image_url").asText() : null;

            return KakaoDTO.builder()
                    .id(id)
                    .accountEmail(email)
                    .profileImageUrl(profileImageUrl)
                    .name(nickname)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse user info from response", e);
            throw new IllegalArgumentException("Failed to parse user info", e);
        }
    }

    private void authenticateUser(User user) {
        UserDetails userDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}