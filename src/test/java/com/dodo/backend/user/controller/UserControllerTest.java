package com.dodo.backend.user.controller;

import com.dodo.backend.user.dto.response.UserResponse.NicknameCheckResponse;
import com.dodo.backend.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * {@link UserController}의 HTTP 요청 처리 로직을 검증하는 테스트 클래스입니다.
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    /**
     * 닉네임 중복 확인 요청 시 200 상태 코드와 중복 확인 결과를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("닉네임 중복 확인 성공")
    void checkNicknameDuplicationSuccessTest() {
        //given
        UserController userController = new UserController(userService);
        String nickname = "도도";
        NicknameCheckResponse serviceResponse = NicknameCheckResponse.toDto(nickname, false);

        given(userService.checkNicknameDuplication(nickname)).willReturn(serviceResponse);

        //when
        ResponseEntity<NicknameCheckResponse> response = userController.checkNicknameDuplication(nickname);

        //then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getNickname()).isEqualTo(nickname);
        assertThat(response.getBody().getDuplicated()).isFalse();

        verify(userService).checkNicknameDuplication(nickname);
    }
}
