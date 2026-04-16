package com.dodo.backend.board.controller;

import com.dodo.backend.board.service.BoardService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link BoardController}의 HTTP 요청 처리 로직을 검증하는 테스트 클래스입니다.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class BoardControllerTest {

    @Mock
    private BoardService boardService;

    /**
     * 게시글 작성 요청 시 201 상태코드와 생성된 게시글 ID를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("게시글 작성 성공: 201 상태코드와 게시글 ID를 반환한다.")
    void createBoard_Success() throws Exception {
        log.info("테스트 시작: 게시글 작성 API 성공");

        // given
        BoardController boardController = new BoardController(boardService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(boardController).build();

        UUID userId = UUID.randomUUID();

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of());

        String requestBody = """
                {
                  "boardTitle": "저희 강아지 자랑합니다!",
                  "boardContent": "오늘 산책하다 찍은 사진이에요. 너무 귀엽죠?",
                  "imageFileUrls": [
                    "https://example.com/image1.jpg",
                    "https://example.com/image2.jpg"
                  ]
                }
                """;

        given(boardService.createBoard(eq(userId), any())).willReturn(1L);

        // when & then
        mockMvc.perform(post("/boards")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("게시글이 성공적으로 작성되었습니다."))
                .andExpect(jsonPath("$.boardId").value(1L));

        log.info("테스트 종료: 게시글 작성 API 성공 검증 완료");
    }
}