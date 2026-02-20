package com.dodo.backend.auth.controller;

import com.dodo.backend.auth.dto.request.AuthRequest;
import com.dodo.backend.auth.dto.response.AuthResponse;
import com.dodo.backend.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 소셜 로그인 흐름을 시각적으로 테스트하기 위한 뷰 컨트롤러입니다.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class TestSocialController {

    private final AuthService authService;

    @Value("${google.client_id}")
    private String googleClientId;

    @Value("${google.redirect_uri}")
    private String googleRedirectUri;

    @Value("${naver.client_id}")
    private String naverClientId;

    @Value("${naver.redirect_uri}")
    private String naverRedirectUri;

    /**
     * 로그인 선택 페이지 진입
     */
    @GetMapping("/view/login")
    public String loginPage(Model model) {
        String googleUrl = "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + googleClientId
                + "&redirect_uri=" + googleRedirectUri
                + "&response_type=code"
                + "&scope=email profile";

        String naverUrl = "https://nid.naver.com/oauth2.0/authorize"
                + "?response_type=code"
                + "&client_id=" + naverClientId
                + "&redirect_uri=" + naverRedirectUri
                + "&state=test_state";

        model.addAttribute("googleUrl", googleUrl);
        model.addAttribute("naverUrl", naverUrl);

        return "login/login-test";
    }

    /**
     * 구글 인증 콜백
     */
    @GetMapping("/google-login")
    public String googleCallback(@RequestParam String code, Model model) {
        log.info("Google OAuth Callback 수신 - code: {}", code);
        return processLogin("GOOGLE", code, model);
    }

    /**
     * 네이버 인증 콜백
     */
    @GetMapping("/naver-login")
    public String naverCallback(@RequestParam("code") String code, @RequestParam("state") String state, Model model) {
        log.info("Naver OAuth Callback 수신 - code: {}, state: {}", code, state);
        return processLogin("NAVER", code, model);
    }

    /**
     * 공통 인증 처리 로직 (Service 호출 및 결과 바인딩)
     */
    private String processLogin(String provider, String code, Model model) {
        AuthRequest.SocialLoginRequest request = AuthRequest.SocialLoginRequest.builder()
                .provider(provider)
                .code(code)
                .build();

        try {
            ResponseEntity<?> responseEntity = authService.socialLogin(request);

            if (responseEntity.getStatusCode().value() == 200) {
                AuthResponse.SocialLoginResponse response = (AuthResponse.SocialLoginResponse) responseEntity.getBody();
                model.addAttribute("result", response);
                model.addAttribute("type", "LOGIN_SUCCESS");
            } else if (responseEntity.getStatusCode().value() == 202) {
                AuthResponse.SocialRegisterResponse response = (AuthResponse.SocialRegisterResponse) responseEntity.getBody();
                model.addAttribute("result", response);
                model.addAttribute("type", "REGISTER_NEEDED");
            }
        } catch (Exception e) {
            log.error("소셜 로그인 테스트 중 오류 발생", e);
            model.addAttribute("error", "로그인 처리 중 서버 오류가 발생했습니다: " + e.getMessage());
        }

        return "login/login-result";
    }
}