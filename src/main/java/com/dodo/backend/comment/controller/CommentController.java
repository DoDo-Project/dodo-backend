package com.dodo.backend.comment.controller;

import com.dodo.backend.comment.dto.request.CommentRequest.CommentCreateRequest;
import com.dodo.backend.comment.dto.request.CommentRequest.CommentUpdateRequest;
import com.dodo.backend.comment.dto.response.CommentResponse.CommentCreateResponse;
import com.dodo.backend.comment.dto.response.CommentResponse.CommentListResponse;
import com.dodo.backend.comment.dto.response.CommentResponse.CommentSimpleResponse;
import com.dodo.backend.comment.service.CommentService;
import com.dodo.backend.common.exception.ErrorResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 댓글 작성, 목록 조회, 수정, 삭제 API를 제공하는 컨트롤러입니다.
 * <p>
 * 인증된 사용자의 UUID는 {@link UserDetails#getUsername()}에서 추출하여 서비스 계층으로 전달합니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/comments")
@Tag(name = "Comment API", description = "댓글 관련 API")
@Slf4j
public class CommentController {

    private final CommentService commentService;

    /**
     * 댓글을 작성합니다.
     *
     * @param request     댓글 작성 요청 DTO
     * @param userDetails 인증된 사용자 정보
     * @return 댓글 작성 응답
     */
    @Operation(summary = "댓글 작성", description = "게시글에 댓글 또는 대댓글을 작성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "댓글이 성공적으로 작성되었습니다.",
                    content = @Content(schema = @Schema(implementation = CommentCreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<CommentCreateResponse> createComment(
            @Valid @RequestBody CommentCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("댓글 작성 요청 수신 - User: {}, BoardId: {}", userId, request.getBoardId());

        return ResponseEntity.ok(commentService.createComment(userId, request));
    }

    /**
     * 특정 게시글의 댓글 목록을 조회합니다.
     *
     * @param boardId     댓글을 조회할 게시글 ID
     * @param page        페이지 번호
     * @param size        페이지 크기
     * @param userDetails 인증된 사용자 정보
     * @return 댓글 목록 조회 응답
     */
    @Operation(summary = "댓글 목록 조회", description = "boardId 기준으로 댓글 목록을 페이지 단위로 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "댓글 목록을 성공적으로 조회했습니다.",
                    content = @Content(schema = @Schema(implementation = CommentListResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 ID의 게시글을 찾을 수 없습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{boardId}")
    public ResponseEntity<CommentListResponse> getComments(
            @PathVariable Long boardId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("댓글 목록 조회 요청 수신 - User: {}, BoardId: {}, Page: {}, Size: {}", userId, boardId, page, size);

        return ResponseEntity.ok(commentService.getComments(boardId, page, size));
    }

    /**
     * 특정 댓글을 수정합니다.
     *
     * @param commentId   수정할 댓글 ID
     * @param request     댓글 수정 요청 DTO
     * @param userDetails 인증된 사용자 정보
     * @return 댓글 수정 응답
     */
    @Operation(summary = "댓글 수정", description = "commentId 기준으로 댓글 내용을 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "댓글이 성공적으로 수정되었습니다.",
                    content = @Content(schema = @Schema(implementation = CommentSimpleResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "댓글을 수정할 권한이 없습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 ID의 댓글을 찾을 수 없습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{commentId}")
    public ResponseEntity<CommentSimpleResponse> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("댓글 수정 요청 수신 - User: {}, CommentId: {}", userId, commentId);

        return ResponseEntity.ok(commentService.updateComment(userId, commentId, request));
    }

    /**
     * 특정 댓글을 삭제 상태로 변경합니다.
     *
     * @param commentId   삭제할 댓글 ID
     * @param userDetails 인증된 사용자 정보
     * @return 댓글 삭제 응답
     */
    @Operation(summary = "댓글 삭제", description = "commentId 기준으로 댓글을 삭제 상태로 변경합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "댓글이 성공적으로 삭제되었습니다.",
                    content = @Content(schema = @Schema(implementation = CommentSimpleResponse.class))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "특정 댓글을 찾을 수 없습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{commentId}")
    public ResponseEntity<CommentSimpleResponse> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("댓글 삭제 요청 수신 - User: {}, CommentId: {}", userId, commentId);

        return ResponseEntity.ok(commentService.deleteComment(userId, commentId));
    }
}
