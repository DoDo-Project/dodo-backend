package com.dodo.backend.reaction.controller;

import com.dodo.backend.common.exception.ErrorResponse;
import com.dodo.backend.reaction.dto.request.ReactionRequest.HistoryReactionCreateRequest;
import com.dodo.backend.reaction.dto.response.ReactionResponse.ReactionSimpleResponse;
import com.dodo.backend.reaction.service.ReactionService;
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
 * 반응(Reaction) 도메인의 HTTP 요청을 처리하는 컨트롤러입니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/reactions")
@Tag(name = "Reaction API", description = "반응 관련 API")
@Slf4j
public class ReactionController {

    private final ReactionService reactionService;

    /**
     * 특정 활동 기록에 반응을 추가합니다.
     *
     * @param userDetails 인증 사용자 정보
     * @param request     반응 추가 요청 DTO
     * @return 처리 결과 메시지 응답
     */
    @Operation(summary = "특정 활동 기록 반응 추가", description = "인증된 사용자가 특정 활동 기록에 반응을 추가합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "반응이 성공적으로 추가되었습니다.",
                    content = @Content(schema = @Schema(implementation = ReactionSimpleResponse.class))),
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
            @ApiResponse(responseCode = "404", description = "활동을 찾을 수 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"활동을 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "409", description = "이미 반응을 누른 활동입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "409 Conflict", value = "{\"status\": 409, \"message\": \"이미 반응을 누른 활동입니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PostMapping("/history")
    public ResponseEntity<ReactionSimpleResponse> createHistoryReaction(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid HistoryReactionCreateRequest request
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("활동 반응 추가 요청 - User: {}, HistoryId: {}", userId, request.getHistoryId());

        ReactionSimpleResponse response = reactionService.createHistoryReaction(userId, request);
        return ResponseEntity.ok(response);
    }
}
