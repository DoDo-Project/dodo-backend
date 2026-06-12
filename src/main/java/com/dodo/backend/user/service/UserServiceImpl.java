package com.dodo.backend.user.service;

import com.dodo.backend.auth.service.RateLimitService;
import com.dodo.backend.common.jwt.JwtTokenProvider;
import com.dodo.backend.mail.service.MailService;
import com.dodo.backend.user.dto.request.UserRequest;
import com.dodo.backend.user.dto.request.UserRequest.UserRegisterRequest;
import com.dodo.backend.user.dto.response.UserResponse.UserInfoResponse;
import com.dodo.backend.user.dto.response.UserResponse.NicknameCheckResponse;
import com.dodo.backend.user.dto.response.UserResponse.UserRegisterResponse;
import com.dodo.backend.user.dto.response.UserResponse.UserUpdateResponse;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.entity.UserRole;
import com.dodo.backend.user.entity.UserStatus;
import com.dodo.backend.user.exception.UserException;
import com.dodo.backend.user.mapper.UserMapper;
import com.dodo.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import static com.dodo.backend.user.entity.UserStatus.*;
import static com.dodo.backend.user.entity.UserStatus.REGISTER;
import static com.dodo.backend.user.exception.UserErrorCode.*;

/**
 * {@link UserService}의 구현체로, 유저 도메인의 비즈니스 로직을 수행합니다.
 * <p>
 * 소셜 로그인 직후의 유저 상태 판별 및 저장,
 * 추가 정보 입력을 통한 회원가입 완료 처리를 담당합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private static final String NICKNAME_PATTERN = "^[가-힣a-zA-Z0-9 ]*$";
    private static final int NICKNAME_MIN_LENGTH = 2;
    private static final int NICKNAME_MAX_LENGTH = 10;

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final MailService mailService;
    private final RateLimitService rateLimitService;

    /**
     * {@inheritDoc}
     * <p>
     * 상세 처리 프로세스:
     * 1. 이메일을 기준으로 DB에서 유저 조회
     * 2. (기존 유저) 유저의 현재 상태(ACTIVE, SUSPENDED 등)와 기본 정보를 반환
     * 3. (신규 유저) 'REGISTER' 상태의 유저 엔티티 생성 및 저장 후 정보 반환
     * <p>
     * 변경 사항: 계정 상태 검증 로직이 AuthService로 이관되었습니다.
     *
     * @return 신규 회원 여부(isNewMember), 유저 식별자, 상태(status) 등을 포함한 Map
     */
    @Transactional
    @Override
    public Map<String, Object> findOrSaveSocialUser(String email, String name, String profileImage) {
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("email", email);
        resultMap.put("name", name);
        resultMap.put("profileUrl", profileImage != null ? profileImage : "");

        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null) {
            log.info("기존 등록된 유저 확인 - 상태: {}", user.getUserStatus());

            resultMap.put("status", user.getUserStatus());
            if (user.getUserStatus() == REGISTER) {
                resultMap.put("isNewMember", true);
            } else {
                resultMap.put("isNewMember", false);
                resultMap.put("userId", user.getUsersId());
                resultMap.put("role", user.getRole().name());
            }

        } else {
            log.info("가입되지 않은 신규 유저 - 정보 생성 및 임시 저장 진행");
            User newUser = User.builder()
                    .email(email)
                    .name(name)
                    .profileUrl(profileImage != null ? profileImage : "")
                    .role(UserRole.USER)
                    .userStatus(REGISTER)
                    .nickname("")
                    .region("")
                    .notificationEnabled(true)
                    .build();

            userRepository.save(newUser);
            resultMap.put("isNewMember", true);
            resultMap.put("status", REGISTER);
        }

        return resultMap;
    }


    /**
     * {@inheritDoc}
     * <p>
     * 상세 처리 프로세스:
     * 1. 유저 조회 및 'REGISTER' 상태(가입 대기) 검증
     * 2. 닉네임 중복 여부 확인 (비즈니스 로직)
     * 3. MyBatis를 통한 정보 업데이트 및 계정 활성화(ACTIVE)
     * 4. 정회원(USER) 권한의 새로운 Access/Refresh 토큰 발급
     * <p>
     * (참고: 입력값의 null/빈값 여부는 컨트롤러단에서 @Valid를 통해 사전에 검증됩니다.)
     *
     * @throws UserException 중복 닉네임, 잘못된 상태, 유저를 찾을 수 없는 경우
     */
    @Transactional
    @Override
    public UserRegisterResponse registerAdditionalInfo(UserRegisterRequest request, String email) {
        log.info("추가 정보 입력 시작");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));

        if (user.getUserStatus() != REGISTER) {
            throw new UserException(INVALID_REQUEST);
        }

        if (userRepository.existsByNickname(request.getNickname())) {
            throw new UserException(NICKNAME_DUPLICATED);
        }

        userMapper.updateUserRegistrationInfo(request.toEntity(email));

        String accessToken = jwtTokenProvider.createAccessToken(user.getUsersId(), "USER");
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUsersId());

        String profileUrl = StringUtils.hasText(request.getProfileUrl()) ? request.getProfileUrl() : user.getProfileUrl();

        return UserRegisterResponse.toDto(profileUrl, "회원가입 성공했습니다.",
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenValidityInMilliseconds());
    }

    /**
     * {@inheritDoc}
     * <p>
     * 상세 처리 프로세스:
     * 1. 전달받은 UUID 식별자를 기반으로 유저 엔티티 조회
     * 2. 식별자 유효성(null) 및 유저의 계정 상태(삭제 여부)를 {@code if} 문으로 검증
     * 3. 유효한 유저일 경우 엔티티를 응답용 DTO로 변환하여 반환
     *
     * @param userId 유저의 고유 UUID 식별자
     * @return 유저의 상세 프로필 정보가 담긴 DTO
     * @throws UserException 유저를 찾을 수 없거나({@code USER_NOT_FOUND}),
     * 잘못된 요청(null 식별자 또는 삭제된 유저)인 경우({@code INVALID_REQUEST}) 발생
     */
    @Transactional(readOnly = true)
    @Override
    public UserInfoResponse getUserInfo(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));

        if (user.getUserStatus() == UserStatus.DELETED || userId == null) {
            throw new UserException(INVALID_REQUEST);
        }

        return UserInfoResponse.toDto(user, "유저 정보 조회 성공했습니다.");
    }

    /**
     * {@inheritDoc}
     * <p>
     * 상세 처리 프로세스:
     * 1. 시스템 내부의 {@link MailService}를 호출하여 탈퇴 인증 번호 발송을 요청
     * 2. 별도의 예외 처리를 수행하지 않으며, 발송 중 발생하는 기술적 예외는
     * 전역 예외 핸들러({@link com.dodo.backend.common.exception.GlobalExceptionHandler})에 의해 {@code INTERNAL_SERVER_ERROR}로 처리
     *
     * @param userId 탈퇴 인증을 진행할 사용자의 Id
     */
    @Transactional
    @Override
    public void requestWithdrawal(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));

        log.info("사용자 탈퇴 인증 메일 발송 시작 - 대상: {}", user.getEmail());

        if (rateLimitService.isEmailCooldownActive(user.getEmail())) {
            log.warn("이메일 발송 제한 (1분 미경과) - Email: {}", user.getEmail());
            throw new UserException(TOO_MANY_REQUESTS);
        }

        String verificationCode = mailService.sendWithdrawalEmail(user.getEmail());
        rateLimitService.saveVerificationCode(user.getEmail(), verificationCode, 5);
        rateLimitService.setEmailCooldown(user.getEmail());

        log.info("인증 메일 발송 및 Redis 저장 완료 - 이메일: {}", user.getEmail());
    }

    /**
     * {@inheritDoc}
     * <p>
     * 상세 처리 프로세스:
     * 1. UUID 기반 유저 조회
     * 2. {@link RateLimitService}를 통해 Redis에 저장된 인증 코드 획득 (getVerificationCode)
     * 3. 입력값과 대조 후 일치하면 'DELETED' 처리 및 관련 Redis 데이터 삭제
     */
    @Transactional
    @Override
    public void deleteWithdrawal(UUID userId, String authCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));

        String savedCode = rateLimitService.getVerificationCode(user.getEmail());

        if (savedCode == null || !savedCode.equals(authCode)) {
            log.warn("탈퇴 인증 실패 - Id: {}, Email: {}, 입력코드: {}", userId, user.getEmail(), authCode);
            throw new UserException(INVALID_REQUEST);
        }

        userMapper.updateUserStatus(userId, UserStatus.DELETED.name());

        rateLimitService.deleteVerificationCode(user.getEmail());
        rateLimitService.deleteEmailCooldown(user.getEmail());

        log.info("회원 탈퇴 처리 성공 - Id: {}, Email: {}", userId, user.getEmail());
    }

    /**
     * {@inheritDoc}
     * <p>
     * 상세 처리 프로세스:
     * 1. UUID 기반 유저 조회 (존재하지 않을 경우 USER_NOT_FOUND 예외 발생)
     * 2. 닉네임 변경 요청이 있을 경우, 기존 닉네임과 다를 때만 중복 검증 수행
     * 3. 변경을 원하는 필드(닉네임, 지역, 가족 여부)가 존재할 경우 엔티티 값 갱신
     * 4. @Transactional에 의한 Dirty Checking으로 메서드 종료 시점에 Update 쿼리 실행
     */
    @Transactional
    @Override
    public UserUpdateResponse updateUserInfo(UUID userId, UserRequest.UserUpdateRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));

        if (request.getNickname() != null && !user.getNickname().equals(request.getNickname())) {
            if (userRepository.existsByNickname(request.getNickname())) {
                log.warn("닉네임 중복 발생 - Id: {}, 중복 시도 닉네임: {}", userId, request.getNickname());
                throw new UserException(NICKNAME_DUPLICATED);
            }
        }

        userMapper.updateUserProfileInfo(request, userId);



        log.info("유저 프로필 수정 성공 - Id: {}", userId);

        return UserUpdateResponse.toDto(
                user,
                "프로필 수정에 성공했습니다.",
                request.getNickname() != null ? request.getNickname() : user.getNickname(),
                request.getRegion() != null ? request.getRegion() : user.getRegion(),
                request.getHasFamily() != null ? request.getHasFamily() : user.getHasFamily(),
                request.getProfileUrl() != null ? request.getProfileUrl() : user.getProfileUrl()
        );
    }

    /**
     * 사용자의 알림 수신 설정(ON/OFF)을 변경합니다.
     * <p>
     * 1. 사용자 조회: 전달받은 userId로 DB에 유저가 존재하는지 확인합니다.
     * 2. 예외 처리: 유저가 존재하지 않을 경우 USER_NOT_FOUND 예외를 발생시킵니다.
     * 3. 정보 갱신: MyBatis 매퍼를 호출하여 해당 유저의 notification_enabled 컬럼 값을 업데이트합니다.
     *
     * @param userId  설정을 변경할 사용자의 고유 식별자(UUID)
     * @param enabled 변경할 알림 수신 여부 (true: 수신 허용, false: 수신 거부)
     */
    @Transactional
    @Override
    public void updateNotification(UUID userId, Boolean enabled) {
        userRepository.findById(userId)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));

        userMapper.updateNotificationStatus(userId, enabled);
    }

    /**
     * 회원가입 전 입력한 닉네임의 형식을 검증하고 중복 여부를 확인합니다.
     * <p>
     * 닉네임은 2자 이상 10자 이하이며, 한글, 영문, 숫자, 공백만 사용할 수 있습니다.
     *
     * @param nickname 중복 여부를 확인할 닉네임
     * @return 닉네임과 중복 여부가 포함된 응답 DTO
     * @throws UserException 닉네임이 비어 있거나 형식이 올바르지 않은 경우
     */
    @Transactional(readOnly = true)
    @Override
    public NicknameCheckResponse checkNicknameDuplication(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new UserException(INVALID_REQUEST);
        }

        if (nickname.length() < NICKNAME_MIN_LENGTH || nickname.length() > NICKNAME_MAX_LENGTH) {
            throw new UserException(INVALID_REQUEST);
        }

        if (!nickname.matches(NICKNAME_PATTERN)) {
            throw new UserException(INVALID_REQUEST);
        }

        boolean duplicated = userRepository.existsByNickname(nickname);

        return NicknameCheckResponse.toDto(nickname, duplicated);
    }

    /**
     * ID로 사용자 엔티티 조회 (예외 처리 포함)
     */
    @Transactional(readOnly = true)
    @Override
    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));
    }
}
