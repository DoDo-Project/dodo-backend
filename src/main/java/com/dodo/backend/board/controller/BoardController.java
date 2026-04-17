package com.dodo.backend.board.controller;

import com.dodo.backend.board.dto.request.BoardRequest.BoardCreateRequest;
import com.dodo.backend.board.dto.response.BoardResponse;
import com.dodo.backend.board.service.BoardService;
import com.dodo.backend.common.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/boards")
@Tag(name = "Board API", description = "게시판 관련 API")
@Slf4j
public class BoardController {

    private final BoardService boardService;

    @Operation(summary = "새로운 게시글 작성", description = "새로운 게시글을 작성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "게시글이 성공적으로 작성되었습니다.",
                    content = @Content(schema = @Schema(implementation = BoardResponse.BoardCreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request",
                                    value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized",
                                    value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "게시글을 생성할 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden",
                                    value = "{\"status\": 403, \"message\": \"게시글을 생성할 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 ID의 게시글을 찾을 수 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found",
                                    value = "{\"status\": 404, \"message\": \"해당 ID의 게시글을 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error",
                                    value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    /**
     * 게시글 작성
     */
    @PostMapping
    public ResponseEntity<BoardResponse.BoardCreateResponse> createBoard(
            @RequestBody BoardCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("게시글 작성 요청 수신 - User: {}, Title: {}", userId, request.getBoardTitle());

        Long boardId = boardService.createBoard(userId, request);

        BoardResponse.BoardCreateResponse response =
                BoardResponse.BoardCreateResponse.toDto(boardId, "게시글이 성공적으로 작성되었습니다.");

        return ResponseEntity.ok(response);
    }
}