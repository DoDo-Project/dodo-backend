package com.dodo.backend.user.controller;

import com.dodo.backend.common.exception.ErrorResponse;
import com.dodo.backend.user.dto.request.UserRequest.NotificationUpdateRequest;
import com.dodo.backend.user.dto.request.UserRequest.UserRegisterRequest;
import com.dodo.backend.user.dto.request.UserRequest.UserUpdateRequest;
import com.dodo.backend.user.dto.request.UserRequest.WithdrawalRequest;
import com.dodo.backend.user.dto.response.UserResponse;
import com.dodo.backend.user.dto.response.UserResponse.NicknameCheckResponse;
import com.dodo.backend.user.dto.response.UserResponse.UserInfoResponse;
import com.dodo.backend.user.dto.response.UserResponse.UserRegisterResponse;
import com.dodo.backend.user.dto.response.UserResponse.UserUpdateResponse;
import com.dodo.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Users API", description = "유저 관련 API")
@RequestMapping("/users")
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * 회원가입 전 입력한 닉네임이 이미 사용 중인지 확인합니다.
     *
     * @param nickname 중복 여부를 확인할 닉네임
     * @return 닉네임과 중복 여부가 포함된 응답 DTO
     */
    @Operation(summary = "닉네임 중복 확인", description = "입력한 닉네임의 중복 여부를 확인합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "닉네임 중복 확인이 완료되었습니다.",
                    content = @Content(schema = @Schema(implementation = NicknameCheckResponse.class))),
            @ApiResponse(responseCode = "400", description = "닉네임 형식이 올바르지 않습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"닉네임 형식이 올바르지 않습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"사용자를 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @GetMapping("/nickname/check")
    public ResponseEntity<NicknameCheckResponse> checkNicknameDuplication(
            @RequestParam String nickname) {

        log.info("닉네임 중복 확인 요청 - nickname: {}", nickname);

        return ResponseEntity.ok(userService.checkNicknameDuplication(nickname));
    }

    /**
     * 회원가입 프로세스의 마지막 단계로, 추가 정보를 입력받아 계정을 활성화합니다.
     * <p>
     * 현재 'REGISTER' 상태인 유저가 닉네임, 지역, 가족여부 필수 정보를 입력하면
     * 상태를 'ACTIVE'로 변경하고, 정회원 권한(USER)을 가진 새로운 토큰을 발급합니다.
     *
     * @param request     닉네임, 전화번호 등 추가 입력 정보가 담긴 DTO
     * @param userDetails SecurityContext에서 추출한 인증 객체 (임시 토큰의 이메일 포함)
     * @return 새로운 Access Token 및 Refresh Token이 포함된 응답 객체
     */
    @Operation(summary = "추가 정보 입력 및 회원가입 완료",
            description = "현재 'REGISTER' 상태인 유저의 추가 정보를 받아 회원가입을 완료합니다. " +
                    "성공 시 계정 상태가 'ACTIVE'로 변경되며, 'USER' 권한을 가진 새로운 Access/Refresh 토큰이 발급됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원가입이 완료되었습니다.",
                    content = @Content(schema = @Schema(implementation = UserRegisterResponse.class))),
            @ApiResponse(responseCode = "400", description = "필수 값이 누락되었거나 형식이 올바르지 않습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"필수 값이 누락되었거나 형식이 올바르지 않습니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 토큰입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"유효하지 않거나 만료된 토큰입니다.\"}"))),
            @ApiResponse(responseCode = "409", description = "이미 사용 중인 닉네임입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "409 Conflict", value = "{\"status\": 409, \"message\": \"이미 사용 중인 닉네임입니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PutMapping("/me/profile")
    public ResponseEntity<UserRegisterResponse> completeRegistration(@Valid @RequestBody UserRegisterRequest request,
                                                                     @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        log.info("회원가입 추가 정보 입력 요청 - 이메일: {}", email);
        return ResponseEntity.ok(userService.registerAdditionalInfo(request, email));
    }

    /**
     * 현재 로그인한 사용자의 상세 정보를 조회합니다.
     *
     * @param userDetails SecurityContext에서 추출한 인증 객체
     * @return 유저의 상세 정보 (이메일, 닉네임, 지역 등)
     */
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 프로필 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "유저 정보 조회 성공했습니다.",
                    content = @Content(schema = @Schema(implementation = UserInfoResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "접근 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"접근 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"사용자를 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> getMyInfo(@AuthenticationPrincipal
                                                                   UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("유저 정보 조회 요청 - Id: {}", userId);

        return ResponseEntity.ok(userService.getUserInfo(userId));
    }

    /**
     * 계정 탈퇴를 위한 본인 인증 메일을 발송합니다.
     * <p>
     * 현재 로그인한 사용자의 이메일로 6자리 인증 번호를 발송하며,
     * 보안을 위해 1분 이내 재요청 시 429 에러를 반환합니다.
     *
     * @param userDetails SecurityContext에서 추출한 인증 객체
     * @return 성공 메시지 (200 OK)
     */
    @Operation(summary = "탈퇴 인증 이메일 발송",
            description = "계정 탈퇴 진행을 위해 현재 로그인한 유저의 이메일로 인증 번호를 발송하고 1분 이내 재요청이 불가능합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "인증 이메일 발송에 성공했습니다.",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "200", value = "{\"message\": \"인증 이메일 발송에 성공했습니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"사용자를 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "429", description = "1분 후 다시 시도해주세요.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "429 Too Many Requests", value = "{\"status\": 429, \"message\": \"잠시 후 다시 시도해주세요.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PostMapping("/me/withdrawal/email")
    public ResponseEntity<String> requestWithdrawalEmail(@AuthenticationPrincipal
                                                         UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("탈퇴 인증 이메일 발송 요청 - Id: {}", userId);
        userService.requestWithdrawal(userId);

        return ResponseEntity.ok("인증 이메일 발송에 성공했습니다.");
    }

    /**
     * 계정 탈퇴 확정 및 인증 번호 검증을 수행합니다.
     * <p>
     * 이메일로 발송된 6자리 인증 코드를 대조하여 본인 확인이 완료되면,
     * 사용자의 상태를 'DELETED'로 변경하고 Redis 내 관련 인증 데이터를 정리합니다.
     *
     * @param request     사용자가 입력한 6자리 인증 번호가 담긴 DTO
     * @param userDetails SecurityContext에서 추출한 현재 인증된 사용자 정보
     * @return 회원 탈퇴 성공 메시지 (200 OK)
     */
    @Operation(summary = "최종 회원 탈퇴",
            description = "발송된 인증 번호를 확인하여 회원 탈퇴를 최종 승인하고 성공 시 계정 상태가 'DELETED'로 변경됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원 탈퇴에 성공했습니다.",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "200", value = "{\"message\": \"회원 탈퇴에 성공했습니다.\"}"))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"사용자를 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @DeleteMapping("/me")
    public ResponseEntity<String> deleteWithdrawal(@RequestBody WithdrawalRequest request,
                                                   @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("최종 탈퇴 요청 - 유저: {}, 코드: {}", userId, request.getAuthCode());
        userService.deleteWithdrawal(userId, request.getAuthCode());

        return ResponseEntity.ok("회원 탈퇴에 성공했습니다.");
    }

    /**
     * 현재 로그인한 사용자의 프로필 정보(닉네임, 지역, 가족 여부)를 수정합니다.
     * <p>
     * 변경을 원하는 필드만 선택적으로 전송할 수 있으며,
     * 닉네임 수정 시 기존 데이터와 중복 여부를 검증합니다.
     *
     * @param request     수정할 정보가 담긴 DTO (nickname, region, hasFamily)
     * @param userDetails SecurityContext에서 추출한 현재 인증된 사용자 정보
     * @return 수정된 최신 사용자 정보 및 성공 메시지 (200 OK)
     */
    @Operation(summary = "내 정보 수정",
            description = "닉네임, 지역, 가족 여부, 프로필 사진 등 내 프로필 정보를 선택적으로 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "프로필 수정에 성공했습니다.",
                    content = @Content(schema = @Schema(implementation = UserUpdateResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"사용자를 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "409", description = "이미 사용중인 닉네임입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "409 Conflict", value = "{\"status\": 409, \"message\": \"이미 사용 중인 닉네임입니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PatchMapping("/me")
    public ResponseEntity<UserUpdateResponse> updateMyInfo(
            @Valid @RequestBody UserUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("유저 정보 수정 요청 - Id: {}, 변경 필드: {}", userId, request);

        return ResponseEntity.ok(userService.updateUserInfo(userId, request));
    }

    /**
     * 현재 로그인한 사용자의 알림 수신 여부를 변경합니다.
     *
     * @param request     변경할 알림 설정 값이 담긴 요청 DTO
     * @param userDetails SecurityContext에서 추출한 현재 인증된 사용자 정보
     * @return 설정 변경 성공 메시지 (200 OK)
     */
    @Operation(summary = "알림 수신 여부 변경", description = "유저의 알림 설정(ON/OFF)을 변경합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "알림 수신 설정을 성공적으로 변경했습니다.",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "200", value = "{\"message\": \"알림 수신 설정을 성공적으로 변경했습니다.\"}"))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"사용자를 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PatchMapping("/me/setting/notification")
    public ResponseEntity<String> updateNotification(
            @Valid @RequestBody NotificationUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        userService.updateNotification(userId, request.getNotificationEnabled());

        return ResponseEntity.ok("알림 수신 설정을 성공적으로 변경했습니다.");
    }
}
