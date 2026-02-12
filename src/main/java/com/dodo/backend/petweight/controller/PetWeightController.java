package com.dodo.backend.petweight.controller;

import com.dodo.backend.common.exception.ErrorResponse;
import com.dodo.backend.petweight.dto.request.PetWeightRequest.PetWeightRegisterRequest;
import com.dodo.backend.petweight.dto.response.PetWeightResponse.PetWeightRegisterResponse;
import com.dodo.backend.petweight.service.PetWeightService;
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

/**
 * 반려동물 체중(PetWeight) 도메인의 HTTP 요청을 처리하는 컨트롤러 클래스입니다.
 * <p>
 * 클라이언트로부터 체중 기록 추가, 조회 등의 요청을 받아 서비스 계층으로 전달하고,
 * 처리 결과를 응답으로 반환합니다.
 */
@RestController
@RequestMapping("/pet/{petId}/weight")
@RequiredArgsConstructor
@Tag(name = "Pet Weight API", description = "반려동물 체중 기록 관리 API")
@Slf4j
public class PetWeightController {

    private final PetWeightService petWeightService;

    /**
     * 특정 반려동물의 새로운 체중 기록을 추가합니다.
     * <p>
     * 인증된 사용자(가족 구성원)만 기록을 추가할 수 있으며,
     * 성공 시 200 OK 상태 코드와 함께 생성된 기록의 ID를 반환합니다.
     *
     * @param petId       체중을 기록할 반려동물의 ID
     * @param request     체중 및 측정 일시 정보가 담긴 요청 객체
     * @param userDetails Spring Security를 통해 인증된 사용자의 세부 정보
     * @return 생성된 기록 ID와 성공 메시지를 포함한 응답 객체 (HTTP 200)
     */
    @Operation(summary = "반려동물 몸무게 기록 추가", description = "특정 반려동물의 체중 기록을 추가합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "반려동물 몸무게 기록 추가를 완료했습니다.",
                    content = @Content(schema = @Schema(implementation = PetWeightRegisterResponse.class))),
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
            @ApiResponse(responseCode = "404", description = "해당 반려동물을 찾을 수 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"해당 반려동물을 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PostMapping
    public ResponseEntity<PetWeightRegisterResponse> addWeight(
            @PathVariable Long petId,
            @RequestBody @Valid PetWeightRegisterRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("체중 기록 추가 요청 - User: {}, PetId: {}, Weight: {}", userId, petId, request.getWeight());

        Long weightId = petWeightService.addWeight(userId, petId, request);

        return ResponseEntity.ok(PetWeightRegisterResponse.toDto(weightId, "반려동물 몸무게 기록 추가를 완료했습니다."));
    }
}