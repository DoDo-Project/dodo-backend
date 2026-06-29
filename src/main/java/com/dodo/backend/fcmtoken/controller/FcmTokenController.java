package com.dodo.backend.fcmtoken.controller;

import com.dodo.backend.fcmtoken.dto.request.FcmTokenRequest.FcmTokenRegisterRequest;
import com.dodo.backend.fcmtoken.dto.response.FcmTokenResponse.FcmTokenSimpleResponse;
import com.dodo.backend.fcmtoken.service.FcmTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/fcm-tokens")
@Tag(name = "FCM Token API", description = "FCM 푸시 토큰 관련 API")
public class FcmTokenController {

    private final FcmTokenService fcmTokenService;

    @Operation(summary = "FCM 푸시 토큰 등록", description = "로그인 사용자의 FCM 푸시 토큰을 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "푸시 토큰이 성공적으로 등록되었습니다.",
                    content = @Content(schema = @Schema(implementation = FcmTokenSimpleResponse.class)))
    })
    @PostMapping
    public ResponseEntity<FcmTokenSimpleResponse> registerToken(
            @Valid @RequestBody FcmTokenRegisterRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("FCM 토큰 등록 요청 - UserId: {}, DeviceType: {}", userId, request.getDeviceType());
        return ResponseEntity.ok(fcmTokenService.registerToken(userId, request));
    }

    @Operation(summary = "FCM 푸시 토큰 삭제", description = "로그인 사용자의 FCM 푸시 토큰을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "푸시 토큰이 성공적으로 삭제되었습니다.",
                    content = @Content(schema = @Schema(implementation = FcmTokenSimpleResponse.class)))
    })
    @DeleteMapping("/{token}")
    public ResponseEntity<FcmTokenSimpleResponse> deleteToken(
            @PathVariable String token,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("FCM 토큰 삭제 요청 - UserId: {}", userId);
        fcmTokenService.deleteToken(userId, token);
        return ResponseEntity.ok(FcmTokenSimpleResponse.toDto("푸시 토큰이 성공적으로 삭제되었습니다."));
    }
}
