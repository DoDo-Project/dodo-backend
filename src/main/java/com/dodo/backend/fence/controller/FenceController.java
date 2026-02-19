package com.dodo.backend.fence.controller;

import com.dodo.backend.common.exception.ErrorResponse;
import com.dodo.backend.fence.dto.request.FenceRequest.FenceRangeRequest;
import com.dodo.backend.fence.dto.request.FenceRequest.FenceRangeUpdateRequest;
import com.dodo.backend.fence.dto.request.FenceRequest.FenceToggleRequest;
import com.dodo.backend.fence.dto.response.FenceResponse.FenceRangeResponse;
import com.dodo.backend.fence.dto.response.FenceResponse.FenceRangeUpdateResponse;
import com.dodo.backend.fence.dto.response.FenceResponse.FenceBoundaryListResponse;
import com.dodo.backend.fence.dto.response.FenceResponse.FenceBoundaryResponse;
import com.dodo.backend.fence.dto.response.FenceResponse.FenceStatusResponse;
import com.dodo.backend.fence.dto.response.FenceResponse.FenceToggleResponse;
import com.dodo.backend.fence.service.FenceService;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 울타리(Fence) 도메인 요청을 처리하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/fence")
@RequiredArgsConstructor
@Tag(name = "Fence API", description = "울타리 설정 관련 API")
@Slf4j
public class FenceController {

    private final FenceService fenceService;

    /**
     * 반려동물의 울타리 거리 범위를 설정합니다.
     *
     * @param request     울타리 설정 요청 정보
     * @param userDetails 인증된 사용자 정보
     * @return 설정 완료 메시지 응답
     */
    @Operation(summary = "거리 범위 설정", description = "반려동물의 울타리 중심 좌표와 반경을 설정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "울타리 설정을 완료했습니다.",
                    content = @Content(schema = @Schema(implementation = FenceRangeResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "해당 반려동물에 대한 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"해당 반려동물에 대한 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 반려동물입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"존재하지 않는 반려동물입니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PostMapping("/range")
    public ResponseEntity<FenceRangeResponse> setFenceRange(
            @Valid @RequestBody FenceRangeRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("울타리 거리 범위 설정 요청 - User: {}, PetId: {}", userId, request.getPetId());

        return ResponseEntity.ok(fenceService.setFenceRange(userId, request));
    }

    /**
     * 반려동물의 울타리 활성화 상태를 조회합니다.
     *
     * @param petId 상태를 조회할 반려동물 ID
     * @return 울타리 활성화 상태 응답
     */
    @Operation(summary = "울타리 상태 조회", description = "특정 반려동물의 울타리 활성화 상태를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "울타리 상태 조회를 완료했습니다.",
                    content = @Content(schema = @Schema(implementation = FenceStatusResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "해당 반려동물에 대한 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"해당 반려동물에 대한 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 반려동물입니다. 또는 존재하지 않는 울타리 정보입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "404 Pet Not Found", value = "{\"status\": 404, \"message\": \"존재하지 않는 반려동물입니다.\"}"),
                                    @ExampleObject(name = "404 Fence Not Found", value = "{\"status\": 404, \"message\": \"존재하지 않는 울타리 정보입니다.\"}")
                            })),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @GetMapping("/{petId}/status")
    public ResponseEntity<FenceStatusResponse> getFenceStatus(
            @PathVariable Long petId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        boolean isDevice = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_DEVICE"::equals);

        if (isDevice) {
            log.info("울타리 상태 조회 요청(디바이스) - Principal: {}, PetId: {}", userDetails.getUsername(), petId);
            return ResponseEntity.ok(fenceService.getFenceStatusForDevice(petId, userDetails.getUsername()));
        }

        log.info("울타리 상태 조회 요청(유저) - User: {}, PetId: {}", userDetails.getUsername(), petId);
        return ResponseEntity.ok(fenceService.getFenceStatus(petId));
    }

    /**
     * 울타리 기능 ON/OFF 상태를 변경합니다.
     *
     * @param fenceId      상태를 변경할 울타리 ID
     * @param request      울타리 상태 변경 요청 정보
     * @param userDetails  인증된 사용자 정보
     * @return 상태 변경 완료 메시지 응답
     */
    @Operation(summary = "울타리 상태 변경", description = "울타리 기능의 ON/OFF 상태를 변경합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "울타리 상태를 변경하는데 성공했습니다.",
                    content = @Content(schema = @Schema(implementation = FenceToggleResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "해당 반려동물에 대한 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"해당 반려동물에 대한 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 반려동물입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"존재하지 않는 반려동물입니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PatchMapping("/{fenceId}/toggle")
    public ResponseEntity<FenceToggleResponse> toggleFence(
            @PathVariable Long fenceId,
            @Valid @RequestBody FenceToggleRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("울타리 상태 변경 요청 - User: {}, FenceId: {}, isActive: {}", userId, fenceId, request.getFenceIsActive());

        return ResponseEntity.ok(fenceService.toggleFence(userId, fenceId, request));
    }

    /**
     * 울타리 범위 정보를 수정합니다.
     *
     * @param fenceId     수정할 울타리 ID
     * @param request     울타리 범위 수정 요청 정보
     * @param userDetails 인증된 사용자 정보
     * @return 울타리 수정 결과 응답
     */
    @Operation(summary = "울타리 범위 수정", description = "울타리 이름, 중심 좌표, 반경 정보를 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "울타리 정보를 수정했습니다.",
                    content = @Content(schema = @Schema(implementation = FenceRangeUpdateResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "해당 울타리에 대한 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"해당 울타리에 대한 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 울타리입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"존재하지 않는 울타리입니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PatchMapping("/{fenceId}/range")
    public ResponseEntity<FenceRangeUpdateResponse> updateFenceRange(
            @PathVariable Long fenceId,
            @Valid @RequestBody FenceRangeUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("울타리 범위 수정 요청 - User: {}, FenceId: {}", userId, fenceId);

        return ResponseEntity.ok(fenceService.updateFenceRange(userId, fenceId, request));
    }

    /**
     * 지도에 표시할 울타리 경계 단건 정보를 조회합니다.
     *
     * @param fenceId     조회할 울타리 ID
     * @param userDetails 인증된 사용자 정보
     * @return 울타리 경계 단건 응답
     */
    @Operation(summary = "울타리 경계 조회", description = "지도에 표시할 울타리 중심 좌표와 반경을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "울타리 정보 조회를 성공했습니다.",
                    content = @Content(schema = @Schema(implementation = FenceBoundaryResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "해당 울타리에 대한 접근 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"해당 울타리에 대한 접근 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 울타리 정보입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"존재하지 않는 울타리 정보입니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @GetMapping("/{fenceId}/boundary")
    public ResponseEntity<FenceBoundaryResponse> getFenceBoundary(
            @PathVariable Long fenceId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("울타리 경계 조회 요청 - User: {}, FenceId: {}", userId, fenceId);

        return ResponseEntity.ok(fenceService.getFenceBoundary(userId, fenceId));
    }

    /**
     * 지도에 표시할 울타리 경계 목록을 조회합니다.
     *
     * @param userDetails 인증된 사용자 정보
     * @return 울타리 경계 목록 응답
     */
    @Operation(summary = "울타리 경계 목록 조회", description = "사용자가 접근 가능한 모든 울타리 중심 좌표와 반경을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "울타리 목록 조회를 성공했습니다.",
                    content = @Content(schema = @Schema(implementation = FenceBoundaryListResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"),
                                    @ExampleObject(name = "400 Fence Already Exists", value = "{\"status\": 400, \"message\": \"이미 울타리가 존재합니다.\"}")
                            })),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "해당 울타리에 대한 접근 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"해당 울타리에 대한 접근 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 울타리 정보입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"존재하지 않는 울타리 정보입니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @GetMapping("/boundaries")
    public ResponseEntity<FenceBoundaryListResponse> getFenceBoundaries(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("울타리 경계 목록 조회 요청 - User: {}", userId);

        return ResponseEntity.ok(fenceService.getFenceBoundaries(userId));
    }
}
