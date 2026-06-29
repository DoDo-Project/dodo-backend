package com.dodo.backend.fcmtoken.controller;

import com.dodo.backend.fcmtoken.dto.request.FcmTokenRequest.FcmTokenRegisterRequest;
import com.dodo.backend.fcmtoken.dto.response.FcmTokenResponse.FcmTokenSimpleResponse;
import com.dodo.backend.fcmtoken.entity.DeviceType;
import com.dodo.backend.fcmtoken.service.FcmTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FcmTokenControllerTest {

    @Mock
    private FcmTokenService fcmTokenService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID userId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FcmTokenController(fcmTokenService))
                .setCustomArgumentResolvers(authenticationPrincipalResolver())
                .build();
        objectMapper = new ObjectMapper();
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("FCM 토큰 등록 API 성공")
    void registerToken_Success() throws Exception {
        given(fcmTokenService.registerToken(eq(userId), any(FcmTokenRegisterRequest.class)))
                .willReturn(FcmTokenSimpleResponse.toDto("푸시 토큰이 성공적으로 등록되었습니다."));

        mockMvc.perform(post("/fcm-tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FcmTokenRegisterRequest("token-value", DeviceType.ANDROID, "Galaxy"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("푸시 토큰이 성공적으로 등록되었습니다."));
    }

    @Test
    @DisplayName("FCM 토큰 삭제 API 성공")
    void deleteToken_Success() throws Exception {
        mockMvc.perform(delete("/fcm-tokens/{token}", "token-value"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("푸시 토큰이 성공적으로 삭제되었습니다."));

        verify(fcmTokenService).deleteToken(userId, "token-value");
    }

    private HandlerMethodArgumentResolver authenticationPrincipalResolver() {
        return new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return User.withUsername(userId.toString()).password("").roles("USER").build();
            }
        };
    }
}
