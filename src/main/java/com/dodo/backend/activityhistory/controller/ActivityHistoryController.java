package com.dodo.backend.activityhistory.controller;

import com.dodo.backend.activityhistory.dto.request.ActivityHistoryRequest;
import com.dodo.backend.activityhistory.dto.request.ActivityHistoryRequest.ActivityCreateRequest;
import com.dodo.backend.activityhistory.dto.request.ActivityHistoryRequest.ActivityStartRequest;
import com.dodo.backend.activityhistory.dto.response.ActivityHistoryResponse;
import com.dodo.backend.activityhistory.dto.response.ActivityHistoryResponse.*;
import com.dodo.backend.activityhistory.service.ActivityHistoryService;
import com.dodo.backend.common.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

import static com.dodo.backend.activityhistory.dto.request.ActivityHistoryRequest.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Activity History API", description = "활동 기록 관련 API")
@RequestMapping("/activities/history")
@Slf4j
public class ActivityHistoryController {

    private final ActivityHistoryService activityHistoryService;

    /**
     * 새로운 활동(산책, 수면 등) 기록을 생성합니다.
     * <p>
     * 이 API는 활동의 '시작 전' 단계에서 호출되며, DB에 초기 상태(BEFORE)로 기록을 생성합니다.
     * </p>
     *
     * @param userDetails SecurityContext에서 추출한 인증 객체 (로그인한 유저)
     * @param request     생성할 활동 정보(반려동물 ID, 활동 유형)가 담긴 요청 DTO
     * @return 생성된 활동 기록의 ID와 유형이 포함된 응답 객체 (HTTP 201 Created)
     */
    @Operation(summary = "활동 기록 생성",
            description = "새로운 활동 기록을 생성하고 초기 상태는 시작 전(BEFORE)으로 설정됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "활동 기록이 성공적으로 생성되었습니다.",
                    content = @Content(schema = @Schema(implementation = ActivityCreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "활동을 기록할 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"활동을 기록할 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "409", description = "진행 중인 활동 기록이 이미 존재합니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "409 Conflict", value = "{\"status\": 409, \"message\": \"진행 중인 활동 기록이 이미 존재합니다.\"}"))),
            @ApiResponse(responseCode = "429", description = "잠시 후 다시 시도해주세요.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "429 Too Many Requests", value = "{\"status\": 429, \"message\": \"잠시 후 다시 시도해주세요.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PostMapping
    public ResponseEntity<ActivityCreateResponse> createActivityHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid ActivityCreateRequest request
    ) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("활동 기록 생성 요청 - User: {}, PetId: {}, Type: {}", userId, request.getPetId(), request.getActivityType());

        ActivityCreateResponse response = activityHistoryService.createActivity(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 활동 기록을 시작하거나 재개합니다.
     * <p>
     * 활동 상태가 '시작 전(BEFORE)'인 경우 <b>최초 시작</b>으로,
     * '취소됨(CANCELED)'인 경우 <b>활동 재개</b>로 처리됩니다.
     * 클라이언트는 사용자의 현재 GPS 좌표를 필수값으로 전달해야 합니다.
     * </p>
     *
     * @param historyId   활동 기록 ID (Path Variable)
     * @param userDetails 인증 객체
     * @param request     시작 위치 정보(위도, 경도) DTO
     * @return 성공 메시지가 담긴 단순 응답 객체
     */
    @Operation(summary = "활동 시작 및 재개",
            description = "대기 중(BEFORE) 또는 중단된(CANCELED) 활동 기록을 시작(IN_PROGRESS)합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "활동 기록이 시작(또는 재개)되었습니다.",
                    content = @Content(schema = @Schema(implementation = ActivitySimpleResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "활동을 시작할 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"활동을 시작할 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 활동 기록을 찾을 수 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"해당 활동 기록을 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "409", description = "이미 활동기록이 진행중입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "409 Conflict", value = "{\"status\": 409, \"message\": \"이미 활동기록이 진행중입니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PatchMapping("/{historyId}/start")
    public ResponseEntity<ActivitySimpleResponse> startActivity(
            @PathVariable Long historyId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid ActivityStartRequest request
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("활동 시작 요청 - User: {}, HistoryId: {}, Lat: {}, Lon: {}",
                userId, historyId, request.getStartLatitude(), request.getStartLongitude());

        ActivitySimpleResponse response = activityHistoryService.startActivity(userId, historyId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * 진행 중인 활동 기록을 중단(CANCELED)합니다.
     * <p>
     * 중단된 활동은 추후 {@code /start} API를 통해 다시 재개할 수 있습니다.
     * </p>
     *
     * @param historyId   활동 기록 ID (Path Variable)
     * @param userDetails 인증 객체
     * @return 성공 메시지가 담긴 단순 응답 객체
     */
    @Operation(summary = "활동 기록 취소",
            description = "진행 중(IN_PROGRESS)인 활동을 중단(CANCELED)하고 종료 시간이 기록됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "활동 기록이 성공적으로 중단되었습니다.",
                    content = @Content(schema = @Schema(implementation = ActivitySimpleResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "해당 활동 기록을 중단할 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"해당 활동 기록을 중단할 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 활동 기록을 찾을 수 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"해당 활동 기록을 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "409", description = "이미 종료된 활동 기록입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "409 Conflict", value = "{\"status\": 409, \"message\": \"이미 종료된 활동 기록입니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PatchMapping("/{historyId}/cancel")
    public ResponseEntity<ActivitySimpleResponse> cancelActivity(
            @PathVariable Long historyId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("활동 중단 요청 - User: {}, HistoryId: {}", userId, historyId);

        ActivitySimpleResponse response = activityHistoryService.cancelActivity(userId, historyId);

        return ResponseEntity.ok(response);
    }

    /**
     * 진행 중인 활동 기록을 종료(COMPLETED)합니다.
     * <p>
     * 클라이언트는 종료 시간과 완료 상태를 전달해야 합니다.
     * </p>
     *
     * @param historyId   활동 기록 ID (Path Variable)
     * @param userDetails 인증 객체
     * @return 종료된 활동 기록의 상세 정보
     */
    @Operation(summary = "활동 기록 종료",
            description = "진행 중(IN_PROGRESS)인 활동을 완료(COMPLETED) 상태로 변경하고 종료 정보를 저장합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "활동 기록이 성공적으로 종료되었습니다.",
                    content = @Content(schema = @Schema(implementation = ActivityFinishResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "해당 활동 기록을 중단할 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"해당 활동 기록을 중단할 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 활동 기록을 찾을 수 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"해당 활동 기록을 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "409", description = "이미 종료된 활동 기록입니다, 아직 기록을 시작하지 않은 활동입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "409 Conflict", value = "{\"status\": 409, \"message\": \"이미 종료된 활동 기록입니다, 아직 기록을 시작하지 않은 활동입니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PatchMapping("/{historyId}/finish")
    public ResponseEntity<ActivityFinishResponse> finishActivity(
            @PathVariable Long historyId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("활동 종료 요청 - User: {}, HistoryId: {}", userId, historyId);

        ActivityFinishResponse response = activityHistoryService.finishActivity(userId, historyId);

        return ResponseEntity.ok(response);
    }

    /**
     * 활동 기록을 삭제합니다.
     * <p>
     * 해당 활동 기록을 영구적으로 삭제하며, 삭제 권한이 없는 경우 403 에러를 반환합니다.
     * 성공 시 200 OK와 함께 성공 메시지를 반환합니다.
     * </p>
     *
     * @param historyId   삭제할 활동 기록 ID (Path Variable)
     * @param userDetails 인증 객체
     * @return 성공 메시지가 담긴 단순 응답 객체
     */
    @Operation(summary = "활동 기록 삭제",
            description = "특정 활동 기록을 영구적으로 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "활동 기록이 성공적으로 삭제되었습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ActivitySimpleResponse.class),
                            examples = @ExampleObject(name = "200 OK", value = "{\"message\": \"활동 기록이 성공적으로 삭제되었습니다.\"}"))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "해당 활동 기록을 삭제할 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"해당 활동 기록을 삭제할 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 활동 기록을 찾을 수 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"해당 활동 기록을 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @DeleteMapping("/{historyId}")
    public ResponseEntity<ActivitySimpleResponse> deleteActivity(
            @PathVariable Long historyId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("활동 삭제 요청 - User: {}, HistoryId: {}", userId, historyId);

        ActivitySimpleResponse response = activityHistoryService.deleteActivity(userId, historyId);
        return ResponseEntity.ok(response);
    }

    /**
     * 내 활동 기록 목록을 조회합니다. (페이지네이션 지원)
     * <p>
     * 페이지 번호(page), 크기(size), 정렬(sort) 조건을 쿼리 파라미터로 받아
     * 페이징된 활동 기록 목록을 반환합니다.
     * </p>
     *
     * @param userDetails 인증 객체 (로그인한 유저)
     * @param pageable    페이징 및 정렬 정보 (기본값: size=10, activityHistoryStartAt 내림차순)
     * @return 페이징된 활동 기록 응답 객체
     */
    @Operation(summary = "내 활동 기록 조회",
            description = "사용자의 활동 기록을 페이징하여 조회합니다. (기본값: size=10, 최신순 정렬)")
    @Parameters({
            @Parameter(name = "page", description = "조회할 페이지 번호 (0부터 시작)", in = ParameterIn.QUERY, example = "0"),
            @Parameter(name = "size", description = "한 페이지에 보여줄 데이터 수", in = ParameterIn.QUERY, example = "10"),
            @Parameter(name = "sort", description = "정렬 기준 (예: activityHistoryStartAt,desc)", in = ParameterIn.QUERY, example = "activityHistoryStartAt,desc")
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "활동 기록 목록을 성공적으로 조회했습니다.",
                    content = @Content(schema = @Schema(implementation = ActivityHistoryPageResponse.class))),
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
            @ApiResponse(responseCode = "404", description = "요청한 페이지를 찾을 수 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"요청한 페이지를 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @GetMapping
    public ResponseEntity<ActivityHistoryPageResponse> getMyActivityHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(hidden = true) @PageableDefault(size = 10, sort = "activityHistoryStartAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("내 활동 기록 조회 요청 - User: {}, Page: {}, Size: {}",
                userId, pageable.getPageNumber(), pageable.getPageSize());

        ActivityHistoryPageResponse response = activityHistoryService.getMyActivityHistory(userId, pageable);

        return ResponseEntity.ok(response);
    }

    /**
     * 주변 인기 활동 기록을 커서 기반으로 조회합니다.
     *
     * @param userDetails  인증 객체
     * @param latitude     현재 위도
     * @param longitude    현재 경도
     * @param limit        요청 크기(기본 10)
     * @param reactionType 정렬 기준 반응 타입(LIKE/DISLIKE)
     * @param cursor       커서(historyId)
     * @return 주변 인기 활동 목록
     */
    @Operation(summary = "주변 인기 활동 조회", description = "반응 타입(LIKE/DISLIKE) 기준으로 주변 인기 활동을 커서 기반 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공적으로 데이터를 조회했습니다.",
                    content = @Content(schema = @Schema(implementation = ActivityHistoryPageResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "주변 활동 기록을 조회할 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"주변 활동 기록을 조회할 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @GetMapping("/popular")
    public ResponseEntity<PopularActivityHistoryResponse> getPopularActivityHistories(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam String reactionType,
            @RequestParam(required = false) Long cursor
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());

        return ResponseEntity.ok(
                activityHistoryService.getPopularActivities(
                        userId,
                        latitude,
                        longitude,
                        limit,
                        reactionType,
                        cursor
                )
        );
    }

    /**
     * 특정 활동의 상세 정보를 조회합니다.
     *
     * @param historyId   조회할 활동 기록 ID
     * @param userDetails 인증된 사용자 정보
     * @return 활동 상세 정보 (HTTP 200)
     */
    @Operation(summary = "활동 상세 정보 조회", description = "특정 활동 기록의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "해당 활동 정보를 성공적으로 조회했습니다.",
                    content = @Content(schema = @Schema(implementation = ActivityHistoryDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "해당 활동 기록을 조회할 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"해당 활동 기록을 조회할 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 ID의 활동 기록을 찾을 수 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"해당 ID의 활동 기록을 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "409", description = "이미 종료된 활동 기록입니다, 아직 기록을 시작하지 않은 활동입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "409 Conflict", value = "{\"status\": 409, \"message\": \"이미 종료된 활동 기록입니다, 아직 기록을 시작하지 않은 활동입니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @GetMapping("/{historyId}")
    public ResponseEntity<ActivityHistoryDetailResponse> getActivityHistoryDetail(
            @PathVariable Long historyId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(activityHistoryService.getActivityHistoryDetail(userId, historyId));
    }

    /**
     * 특정 반려동물의 활동 상태를 조회합니다.
     * <p>
     * 해당 반려동물의 가장 최근 활동 기록을 확인하여 진행 중인지, 시작 전인지 등의 상태를 반환합니다.
     * </p>
     *
     * @param petId       상태를 조회할 반려동물 ID
     * @param userDetails 인증된 사용자 정보
     * @return 반려동물 활동 상태 정보 (HTTP 200)
     */
    @Operation(summary = "반려동물 활동 상태 조회", description = "특정 반려동물의 현재 활동 상태(진행 여부)를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "활동 기록중인 애완동물입니다, 활동 시작 전 상태입니다, 활동이 중단된 상태입니다, 현재 진행 중인 활동이 없습니다.",
                    content = @Content(schema = @Schema(implementation = ActivityStatusResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "해당 반려동물의 정보를 조회할 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"해당 반려동물의 정보를 조회할 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 ID의 반려동물을 찾을 수 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"해당 ID의 반려동물을 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @GetMapping("/{petId}/status")
    public ResponseEntity<ActivityStatusResponse> getPetActivityStatus(
            @PathVariable Long petId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        boolean isDevice = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_DEVICE"::equals);

        if (isDevice) {
            log.info("반려동물 활동 상태 조회 요청(디바이스) - Principal: {}, PetId: {}", userDetails.getUsername(), petId);
            return ResponseEntity.ok(activityHistoryService.getPetActivityStatusForDevice(userDetails.getUsername(), petId));
        }

        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("반려동물 활동 상태 조회 요청(유저) - User: {}, PetId: {}", userId, petId);
        return ResponseEntity.ok(activityHistoryService.getPetActivityStatus(userId, petId));
    }

    /**
     * 활동 기록의 상세 이동 경로(GPS 좌표)를 조회합니다.
     * <p>
     * 특정 활동 기록에 포함된 모든 GPS 좌표 리스트를 반환하여,
     * 클라이언트가 지도상에 이동 경로를 그릴 수 있도록 합니다.
     * </p>
     *
     * @param historyId   조회할 활동 기록 ID (Path Variable)
     * @param userDetails 인증된 사용자 정보
     * @return 상세 경로 및 활동 정보가 담긴 응답 객체 (HTTP 200)
     */
    @Operation(summary = "활동 상세 경로 조회", description = "특정 활동의 이동 경로리스트를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "상세 경로를 가져오는데 성공했습니다.",
                    content = @Content(schema = @Schema(implementation = ActivityRouteResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "해당 활동 기록을 조회할 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"해당 활동 기록을 조회할 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 ID의 활동 기록을 찾을 수 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"해당 ID의 활동 기록을 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @GetMapping("/{historyId}/route")
    public ResponseEntity<ActivityRouteResponse> getActivityRoute(
            @PathVariable Long historyId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("활동 상세 경로 조회 요청 - User: {}, HistoryId: {}", userId, historyId);

        return ResponseEntity.ok(activityHistoryService.getActivityRoute(userId, historyId));
    }
}
