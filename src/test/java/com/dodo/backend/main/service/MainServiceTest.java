package com.dodo.backend.main.service;

import com.dodo.backend.board.entity.Board;
import com.dodo.backend.board.entity.BoardStatus;
import com.dodo.backend.board.entity.BoardType;
import com.dodo.backend.board.repository.BoardRepository;
import com.dodo.backend.healthanalysis.dto.response.HealthAnalysisResponse.AnalysisListItem;
import com.dodo.backend.healthanalysis.dto.response.HealthAnalysisResponse.AnalysisListResponse;
import com.dodo.backend.healthanalysis.service.HealthAnalysisService;
import com.dodo.backend.imagefile.service.ImageFileService;
import com.dodo.backend.main.dto.response.MainResponse.Announcement;
import com.dodo.backend.main.dto.response.MainResponse.HealthReport;
import com.dodo.backend.main.dto.response.MainResponse.MainPageResponse;
import com.dodo.backend.main.dto.response.MainResponse.PetProfile;
import com.dodo.backend.pet.dto.response.PetResponse.PetListResponse;
import com.dodo.backend.pet.dto.response.PetResponse.PetListResponse.PetSummary;
import com.dodo.backend.pet.service.PetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * {@link MainService}의 비즈니스 로직을 검증하는 테스트 클래스입니다.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class MainServiceTest {

    @InjectMocks
    private MainServiceImpl mainService;

    @Mock
    private PetService petService;

    @Mock
    private HealthAnalysisService healthAnalysisService;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private ImageFileService imageFileService;

    @Spy
    private ObjectMapper objectMapper;

    /**
     * 반려동물과 최신 건강 리포트가 존재할 때 메인 페이지 정보를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("메인 페이지 조회 성공: 반려동물과 건강 리포트가 포함된다.")
    void getMainPage_WithPetProfilesAndHealthReports() {
        log.info("테스트 시작: 반려동물과 건강 리포트가 포함된 메인 페이지 조회");

        // given
        UUID userId = UUID.randomUUID();
        PetSummary petSummary = PetSummary.builder()
                .petId(1L)
                .petName("보리")
                .imageFileUrl("https://example.com/pets/1.png")
                .species("CANINE")
                .breed("POODLE")
                .sex("FEMALE")
                .age(3)
                .weight(4.5)
                .build();

        PetListResponse petListResponse = PetListResponse.builder()
                .message("반려동물 목록 조회 성공")
                .pets(List.of(petSummary))
                .totalPages(1)
                .totalElements(1L)
                .currentPage(0)
                .pageSize(10)
                .build();

        Map<String, Object> fullContent = Map.of("score", 95);
        LocalDateTime analysisDate = LocalDateTime.of(2025, 1, 15, 10, 30);
        AnalysisListItem analysisListItem = AnalysisListItem.toDto(
                101L,
                "월간 리포트",
                "건강 요약",
                fullContent,
                analysisDate,
                "MONTHLY",
                "COMPLETED"
        );

        AnalysisListResponse analysisListResponse = AnalysisListResponse.builder()
                .message("건강 분석 결과 조회 성공")
                .pageInfo(null)
                .data(List.of(analysisListItem))
                .build();

        given(petService.getPetList(userId, Pageable.unpaged())).willReturn(petListResponse);
        given(healthAnalysisService.getAnalysisList(userId, petSummary.getPetId(), 0, 1, null))
                .willReturn(analysisListResponse);
        Board notice = Board.builder()
                .boardId(11L)
                .boardTitle("notice title")
                .boardContent("notice content")
                .viewCount(10)
                .boardStatus(BoardStatus.PUBLISHED)
                .boardType(BoardType.NOTICE)
                .build();
        given(boardRepository.findByBoardTypeAndBoardStatusOrderByBoardCreatedAtDesc(
                BoardType.NOTICE,
                BoardStatus.PUBLISHED,
                PageRequest.of(0, 3)
        )).willReturn(List.of(notice));
        given(imageFileService.getBoardImageUrls(notice.getBoardId()))
                .willReturn(List.of("https://example.com/notice/11.png"));

        // when
        MainPageResponse response = mainService.getMainPage(userId);

        // then
        assertNotNull(response);
        List<PetProfile> petProfiles = response.getPetProfiles();
        assertEquals(1, petProfiles.size());

        PetProfile profile = petProfiles.get(0);
        assertEquals(petSummary.getPetId(), profile.getPetId());
        assertEquals(petSummary.getPetName(), profile.getName());
        assertEquals(petSummary.getImageFileUrl(), profile.getImageFileUrl());
        assertEquals(petSummary.getBreed(), profile.getBreed());
        assertEquals(petSummary.getAge(), profile.getAge());
        assertEquals(petSummary.getSpecies(), profile.getSpercies());
        assertEquals(petSummary.getSex(), profile.getSex());
        assertEquals(petSummary.getWeight(), profile.getWeight());

        List<HealthReport> healthReports = response.getHealthReports();
        assertEquals(1, healthReports.size());

        HealthReport healthReport = healthReports.get(0);
        assertEquals(analysisListItem.getAnalysisId(), healthReport.getDashboardId());
        assertEquals(analysisListItem.getHealthAnalysisTitle(), healthReport.getHealthReportTitle());
        assertEquals(analysisListItem.getHealthAnalysisSummary(), healthReport.getHealthReportSummary());
        assertEquals("{\"score\":95}", healthReport.getHealthReportContent());
        assertEquals(analysisDate.toLocalDate(), healthReport.getCheckupDate());
        assertEquals(petSummary.getPetName(), healthReport.getPetName());

        List<Announcement> announcements = response.getAnnouncement();
        assertEquals(1, announcements.size());
        Announcement announcement = announcements.get(0);
        assertEquals(notice.getBoardTitle(), announcement.getBoardTitle());
        assertEquals(notice.getBoardContent(), announcement.getBoardContent());
        assertEquals("https://example.com/notice/11.png", announcement.getImageFileUrl());
        assertEquals(notice.getViewCount(), announcement.getViewCount());

        verify(petService).getPetList(userId, Pageable.unpaged());
        verify(healthAnalysisService).getAnalysisList(userId, petSummary.getPetId(), 0, 1, null);
        verify(boardRepository).findByBoardTypeAndBoardStatusOrderByBoardCreatedAtDesc(
                BoardType.NOTICE,
                BoardStatus.PUBLISHED,
                PageRequest.of(0, 3)
        );
        verify(imageFileService).getBoardImageUrls(notice.getBoardId());

        log.info("테스트 종료: 메인 페이지 응답 검증 완료");
    }

    /**
     * 반려동물 정보가 없을 때 빈 목록과 기본 메세지를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("메인 페이지 조회: 반려동물이 없으면 빈 리스트 반환")
    void getMainPage_WithoutPets_ReturnsEmptyLists() {
        log.info("테스트 시작: 반려동물 없음 - 빈 메인 페이지 응답");

        // given
        UUID userId = UUID.randomUUID();
        PetListResponse petListResponse = PetListResponse.builder()
                .message("반려동물 없음")
                .pets(Collections.emptyList())
                .totalPages(0)
                .totalElements(0L)
                .currentPage(0)
                .pageSize(0)
                .build();

        given(petService.getPetList(userId, Pageable.unpaged())).willReturn(petListResponse);
        given(boardRepository.findByBoardTypeAndBoardStatusOrderByBoardCreatedAtDesc(
                BoardType.NOTICE,
                BoardStatus.PUBLISHED,
                PageRequest.of(0, 3)
        )).willReturn(Collections.emptyList());

        // when
        MainPageResponse response = mainService.getMainPage(userId);

        // then
        assertNotNull(response);
        assertTrue(response.getPetProfiles().isEmpty());
        assertTrue(response.getHealthReports().isEmpty());
        assertTrue(response.getAnnouncement().isEmpty());

        verify(petService).getPetList(userId, Pageable.unpaged());
        verify(boardRepository).findByBoardTypeAndBoardStatusOrderByBoardCreatedAtDesc(
                BoardType.NOTICE,
                BoardStatus.PUBLISHED,
                PageRequest.of(0, 3)
        );
        verifyNoInteractions(healthAnalysisService);
        verifyNoInteractions(imageFileService);

        log.info("테스트 종료: 반려동물 없음 응답 검증 완료");
    }

    /**
     * 반려동물은 존재하지만 건강 분석 이력이 없을 때 건강 리포트가 비어 있는지 검증합니다.
     */
    @Test
    @DisplayName("메인 페이지 조회: 건강 분석 이력이 없으면 건강 리포트 목록이 비어있다.")
    void getMainPage_PetWithoutHealthAnalysis() {
        log.info("테스트 시작: 건강 분석 이력 없음");

        // given
        UUID userId = UUID.randomUUID();
        PetSummary petSummary = PetSummary.builder()
                .petId(7L)
                .petName("차우")
                .imageFileUrl("https://example.com/pets/7.png")
                .species("FELINE")
                .breed("RAGDOLL")
                .sex("MALE")
                .age(2)
                .weight(4.1)
                .build();

        PetListResponse petListResponse = PetListResponse.builder()
                .message("반려동물 조회 성공")
                .pets(List.of(petSummary))
                .totalPages(1)
                .totalElements(1L)
                .currentPage(0)
                .pageSize(10)
                .build();

        AnalysisListResponse emptyAnalysisResponse = AnalysisListResponse.builder()
                .message("건강 분석 없음")
                .pageInfo(null)
                .data(Collections.emptyList())
                .build();

        given(petService.getPetList(userId, Pageable.unpaged())).willReturn(petListResponse);
        given(healthAnalysisService.getAnalysisList(userId, petSummary.getPetId(), 0, 1, null))
                .willReturn(emptyAnalysisResponse);
        given(boardRepository.findByBoardTypeAndBoardStatusOrderByBoardCreatedAtDesc(
                BoardType.NOTICE,
                BoardStatus.PUBLISHED,
                PageRequest.of(0, 3)
        )).willReturn(Collections.emptyList());

        // when
        MainPageResponse response = mainService.getMainPage(userId);

        // then
        assertNotNull(response);
        assertEquals(1, response.getPetProfiles().size());
        assertTrue(response.getHealthReports().isEmpty());
        assertTrue(response.getAnnouncement().isEmpty());

        verify(petService).getPetList(userId, Pageable.unpaged());
        verify(healthAnalysisService).getAnalysisList(userId, petSummary.getPetId(), 0, 1, null);
        verify(boardRepository).findByBoardTypeAndBoardStatusOrderByBoardCreatedAtDesc(
                BoardType.NOTICE,
                BoardStatus.PUBLISHED,
                PageRequest.of(0, 3)
        );

        log.info("테스트 종료: 건강 분석 이력 없음 응답 검증 완료");
    }
}
