package com.dodo.backend.petweight.controller;

import com.dodo.backend.common.exception.ErrorResponse;
import com.dodo.backend.petweight.dto.request.PetWeightRequest;
import com.dodo.backend.petweight.dto.request.PetWeightRequest.PetWeightRegisterRequest;
import com.dodo.backend.petweight.dto.request.PetWeightRequest.PetWeightUpdateRequest;
import com.dodo.backend.petweight.dto.response.PetWeightResponse;
import com.dodo.backend.petweight.dto.response.PetWeightResponse.PetWeightDeleteResponse;
import com.dodo.backend.petweight.dto.response.PetWeightResponse.PetWeightHistoryResponse;
import com.dodo.backend.petweight.dto.response.PetWeightResponse.PetWeightRegisterResponse;
import com.dodo.backend.petweight.dto.response.PetWeightResponse.PetWeightUpdateResponse;
import com.dodo.backend.petweight.service.PetWeightService;
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

    /**
     * 반려동물의 체중 기록을 페이징하여 조회합니다.
     * <p>
     * 기본적으로 측정 날짜(petWeightsMeasuredAt) 내림차순(최신순)으로 정렬됩니다.
     * 페이지 번호(page)는 0부터 시작하며, 한 페이지당 기본 10개의 데이터를 반환합니다.
     *
     * @param petId       조회할 반려동물의 ID
     * @param pageable    페이징 정보 (page, size, sort)
     * @param userDetails 인증된 사용자 정보
     * @return 페이징된 체중 기록 목록과 페이지 메타데이터 (HTTP 200)
     */
    @Operation(summary = "반려동물 몸무게 기록 조회", description = "특정 반려동물의 체중 기록 이력을 페이징하여 조회합니다.")
    @Parameters({
            @Parameter(name = "page", description = "조회할 페이지 번호 (0부터 시작)", in = ParameterIn.QUERY, example = "0"),
            @Parameter(name = "size", description = "한 페이지에 보여줄 데이터 수", in = ParameterIn.QUERY, example = "10"),
            @Parameter(name = "sort", description = "정렬 기준 (예: petWeightsMeasuredAt,desc)", in = ParameterIn.QUERY, example = "petWeightsMeasuredAt,desc")
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회를 성공했습니다.",
                    content = @Content(schema = @Schema(implementation = PetWeightHistoryResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "조회 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"해당 반려동물에 대한 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "애완동물을 찾을 수 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"해당 반려동물을 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @GetMapping("/history")
    public ResponseEntity<PetWeightHistoryResponse> getWeightHistory(
            @PathVariable Long petId,
            @Parameter(hidden = true) @PageableDefault(size = 10, sort = "petWeightsMeasuredAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        PetWeightHistoryResponse response = petWeightService.getWeightHistory(userId, petId, pageable);

        return ResponseEntity.ok(response);
    }

    /**
     * 기존 반려동물의 체중 기록을 수정합니다.
     * <p>
     * 변경하고 싶은 필드만 요청 본문에 포함하여 전송합니다 (Dynamic Update).
     * 값이 null인 필드는 기존 데이터를 유지합니다.
     *
     * @param petId       반려동물 ID
     * @param weightId    수정할 체중 기록의 고유 ID
     * @param request     수정할 정보 (몸무게, 날짜 - 둘 다 선택 사항)
     * @param userDetails 인증된 사용자 정보
     * @return 수정 완료 메시지 (HTTP 200)
     */
    @Operation(summary = "반려동물 몸무게 기록 수정", description = "기존에 등록된 체중 기록을 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "몸무게 기록 수정을 완료했습니다.",
                    content = @Content(schema = @Schema(implementation = PetWeightUpdateResponse.class))),
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
            @ApiResponse(responseCode = "404", description = "해당 몸무게 기록을 찾을 수 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"해당 몸무게 기록을 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PatchMapping("/{weightId}")
    public ResponseEntity<PetWeightUpdateResponse> updateWeight(
            @PathVariable Long petId,
            @PathVariable Long weightId,
            @RequestBody @Valid PetWeightUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("체중 수정 요청 - User: {}, PetId: {}, WeightId: {}", userId, petId, weightId);

        petWeightService.updateWeight(userId, petId, weightId, request);

        return ResponseEntity.ok(PetWeightUpdateResponse.toDto("몸무게 기록 수정을 완료했습니다."));
    }

    /**
     * 기존 반려동물의 체중 기록을 삭제합니다.
     * <p>
     * 삭제된 데이터는 복구할 수 없으며, 요청한 사용자가 해당 반려동물의 가족 구성원이어야 합니다.
     *
     * @param petId       반려동물 ID
     * @param weightId    삭제할 체중 기록의 고유 ID
     * @param userDetails 인증된 사용자 정보
     * @return 삭제 완료 메시지 (HTTP 200)
     */
    @Operation(summary = "반려동물 몸무게 기록 삭제", description = "기존에 등록된 체중 기록을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "몸무게 기록 삭제를 완료했습니다.",
                    content = @Content(schema = @Schema(implementation = PetWeightDeleteResponse.class))),
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
            @ApiResponse(responseCode = "404", description = "해당 몸무게 기록을 찾을 수 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"해당 몸무게 기록을 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @DeleteMapping("/{weightId}")
    public ResponseEntity<PetWeightDeleteResponse> deleteWeight(
            @PathVariable Long petId,
            @PathVariable Long weightId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("체중 삭제 요청 - User: {}, PetId: {}, WeightId: {}", userId, petId, weightId);

        petWeightService.deleteWeight(userId, petId, weightId);

        return ResponseEntity.ok(PetWeightDeleteResponse.toDto("몸무게 기록 삭제를 완료했습니다."));
    }
}