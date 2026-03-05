package com.dodo.backend.imagefile.controller;

import com.dodo.backend.common.exception.ErrorResponse;
import com.dodo.backend.imagefile.dto.response.ImageFileResponse.ImageUploadResponse;
import com.dodo.backend.imagefile.service.ImageFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 이미지 파일 업로드 API 컨트롤러입니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/files")
@Tag(name = "Image File API", description = "이미지 파일 업로드 API")
public class ImageFileController {

    private final ImageFileService imageFileService;

    /**
     * 이미지 파일을 Cloudinary에 업로드하고 URL 목록을 반환합니다.
     */
    @Operation(summary = "이미지 업로드", description = "여러 장의 이미지 파일을 업로드하고 URL 목록을 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "이미지 업로드에 성공하였습니다.",
                    content = @Content(schema = @Schema(implementation = ImageUploadResponse.class))),
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
            @ApiResponse(responseCode = "404", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageUploadResponse> uploadImages(
            @RequestParam("files") List<MultipartFile> files
    ) {
        return ResponseEntity.ok(imageFileService.uploadImages(files));
    }
}
