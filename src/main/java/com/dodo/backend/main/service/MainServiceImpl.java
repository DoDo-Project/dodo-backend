package com.dodo.backend.main.service;

import com.dodo.backend.board.entity.Board;
import com.dodo.backend.board.entity.BoardStatus;
import com.dodo.backend.board.entity.BoardType;
import com.dodo.backend.board.repository.BoardRepository;
import com.dodo.backend.healthanalysis.dto.response.HealthAnalysisResponse.AnalysisListItem;
import com.dodo.backend.healthanalysis.dto.response.HealthAnalysisResponse.AnalysisListResponse;
import com.dodo.backend.healthanalysis.service.HealthAnalysisService;
import com.dodo.backend.imagefile.service.ImageFileService;
import com.dodo.backend.main.dto.response.MainResponse.*;
import com.dodo.backend.pet.dto.response.PetResponse.PetListResponse;
import com.dodo.backend.pet.dto.response.PetResponse.PetListResponse.PetSummary;
import com.dodo.backend.pet.service.PetService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 메인 페이지 서비스 구현체입니다.
 */
@Service
@RequiredArgsConstructor
public class MainServiceImpl implements MainService {

    private static final int MAIN_ANNOUNCEMENT_LIMIT = 3;

    private final PetService petService;
    private final HealthAnalysisService healthAnalysisService;
    private final BoardRepository boardRepository;
    private final ImageFileService imageFileService;
    private final ObjectMapper objectMapper;

    /**
     * 로그인한 사용자의 메인 페이지 정보를 조회합니다.
     *
     * @param userId 인증된 사용자 ID
     * @return 메인 페이지 응답 DTO
     */
    @Transactional(readOnly = true)
    @Override
    public MainPageResponse getMainPage(UUID userId) {
        List<PetProfile> petProfiles = buildPetProfiles(userId);
        List<HealthReport> healthReports = buildHealthReports(userId, petProfiles);
        List<Announcement> announcements = buildAnnouncements();

        return MainPageResponse.toDto(
                "메인 페이지 정보를 가져오는데 성공했습니다.",
                petProfiles,
                healthReports,
                announcements
        );
    }

    /**
     * 펫 도메인 서비스만 사용하여 메인에 필요한 프로필 목록을 생성합니다.
     *
     * @param userId 인증된 사용자 ID
     * @return 반려동물 프로필 목록
     */
    private List<PetProfile> buildPetProfiles(UUID userId) {
        PetListResponse petListResponse = petService.getPetList(userId, Pageable.unpaged());
        List<PetSummary> pets = petListResponse.getPets();

        if (pets == null || pets.isEmpty()) {
            return Collections.emptyList();
        }

        return pets.stream()
                .map(pet -> PetProfile.builder()
                        .petId(pet.getPetId())
                        .name(pet.getPetName())
                        .imageFileUrl(pet.getImageFileUrl())
                        .breed(pet.getBreed())
                        .age(pet.getAge())
                        .spercies(pet.getSpecies())
                        .sex(pet.getSex())
                        .weight(pet.getWeight())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 각 반려동물의 최신 건강 분석 리포트를 요약 목록으로 만듭니다.
     *
     * @param userId      인증된 사용자 ID
     * @param petProfiles 반려동물 프로필 목록
     * @return 건강 리포트 목록
     */
    private List<HealthReport> buildHealthReports(UUID userId, List<PetProfile> petProfiles) {
        if (petProfiles == null || petProfiles.isEmpty()) {
            return Collections.emptyList();
        }

        return petProfiles.stream()
                .map(profile -> buildLatestHealthReport(userId, profile))
                .filter(report -> report != null)
                .collect(Collectors.toList());
    }

    /**
     * 반려동물의 최신 건강 분석 리포트를 불러와 요약 DTO로 변환합니다.
     *
     * @param userId  인증된 사용자 ID
     * @param profile 반려동물 프로필
     * @return 건강 리포트 요약(없으면 null)
     */
    private HealthReport buildLatestHealthReport(UUID userId, PetProfile profile) {
        AnalysisListResponse response = healthAnalysisService.getAnalysisList(
                userId,
                profile.getPetId(),
                0,
                1,
                null
        );

        List<AnalysisListItem> items = response.getData();
        if (items == null || items.isEmpty()) {
            return null;
        }

        AnalysisListItem item = items.get(0);
        String content = extractRecommendations(item.getHealthAnalysisFullContent());

        return HealthReport.builder()
                .petId(profile.getPetId())
                .petName(profile.getName())
                .dashboardId(item.getAnalysisId())
                .healthReportTitle(item.getHealthAnalysisTitle())
                .healthReportSummary(item.getHealthAnalysisSummary())
                .healthReportContent(content)
                .checkupDate(item.getAnalysisDate() == null ? null : item.getAnalysisDate().toLocalDate())
                .build();
    }

    /**
     * 분석 상세 내용에서 메인에 노출할 권장 행동만 추출합니다.
     *
     * @param content 분석 상세 객체
     * @return 권장 행동 문자열
     */
    private String extractRecommendations(Object content) {
        if (content == null) {
            return "";
        }

        if (content instanceof Map<?, ?> contentMap) {
            return extractRecommendationsFromMap(contentMap);
        }

        if (content instanceof String value) {
            try {
                Map<?, ?> contentMap = objectMapper.readValue(value, Map.class);
                return extractRecommendationsFromMap(contentMap);
            } catch (JsonProcessingException e) {
                return "";
            }
        }

        return "";
    }

    private String extractRecommendationsFromMap(Map<?, ?> contentMap) {
        Object recommendations = contentMap.get("recommendations");
        if (recommendations instanceof List<?> list) {
            return list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .filter(value -> !value.isBlank())
                    .collect(Collectors.joining("\n"));
        }

        Object recommendation = contentMap.get("recommendation");
        if (recommendation instanceof String value) {
            return value;
        }

        return "";
    }

    /**
     * 메인 페이지에 노출할 최신 공지사항 목록을 조회합니다.
     * <p>
     * 공개 상태의 공지 게시글만 최신순으로 제한 개수만큼 조회합니다.
     *
     * @return 공지사항 요약 목록
     */
    private List<Announcement> buildAnnouncements() {
        return boardRepository.findByBoardTypeAndBoardStatusOrderByBoardCreatedAtDesc(
                        BoardType.NOTICE,
                        BoardStatus.PUBLISHED,
                        PageRequest.of(0, MAIN_ANNOUNCEMENT_LIMIT)
                )
                .stream()
                .map(this::toAnnouncement)
                .collect(Collectors.toList());
    }

    /**
     * 공지 게시글 엔티티를 메인 페이지 공지사항 요약 DTO로 변환합니다.
     * <p>
     * 게시글 이미지가 여러 개일 경우 첫 번째 이미지 URL만 대표 이미지로 사용합니다.
     *
     * @param board 공지 게시글 엔티티
     * @return 공지사항 요약 DTO
     */
    private Announcement toAnnouncement(Board board) {
        List<String> imageFileUrls = imageFileService.getBoardImageUrls(board.getBoardId());
        String imageFileUrl = imageFileUrls.isEmpty() ? null : imageFileUrls.get(0);

        return Announcement.builder()
                .boardTitle(board.getBoardTitle())
                .boardContent(board.getBoardContent())
                .imageFileUrl(imageFileUrl)
                .viewCount(board.getViewCount())
                .build();
    }
}
