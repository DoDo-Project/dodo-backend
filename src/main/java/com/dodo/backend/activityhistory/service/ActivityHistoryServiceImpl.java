package com.dodo.backend.activityhistory.service;

import com.dodo.backend.activityhistory.dto.request.ActivityHistoryRequest;
import com.dodo.backend.activityhistory.dto.request.ActivityHistoryRequest.ActivityCreateRequest;
import com.dodo.backend.activityhistory.dto.request.ActivityHistoryRequest.ActivityStartRequest;
import com.dodo.backend.activityhistory.dto.response.ActivityHistoryResponse;
import com.dodo.backend.activityhistory.dto.response.ActivityHistoryResponse.*;
import com.dodo.backend.activityhistory.entity.ActivityHistory;
import com.dodo.backend.activityhistory.entity.ActivityHistoryStatus;
import com.dodo.backend.activityhistory.exception.ActivityHistoryException;
import com.dodo.backend.activityhistory.mapper.ActivityHistoryMapper;
import com.dodo.backend.activityhistory.repository.ActivityHistoryRepository;
import com.dodo.backend.imagefile.service.ImageFileService;
import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.pet.service.PetService;
import com.dodo.backend.routepoint.service.RoutePointService;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.service.UserService;
import com.dodo.backend.userpet.service.UserPetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.dodo.backend.activityhistory.dto.request.ActivityHistoryRequest.*;
import static com.dodo.backend.activityhistory.exception.ActivityHistoryErrorCode.*;

/**
 * {@link ActivityHistoryService} ?명꽣?섏씠?ㅼ쓽 援ы쁽泥??대옒?ㅼ엯?덈떎.
 * <p>
 * 諛섎젮?숇Ъ???쒕룞 湲곕줉(ActivityHistory)???앹꽦(Create), ?쒖옉(Start/Resume), 以묐떒(Cancel), 醫낅즺(Finish) ??
 * ?쒕룞 ?앸챸二쇨린瑜?愿由ы븯???듭떖 鍮꾩쫰?덉뒪 濡쒖쭅???섑뻾?⑸땲??
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityHistoryServiceImpl implements ActivityHistoryService {

    private final ActivityHistoryRepository activityHistoryRepository;
    private final PetService petService;
    private final UserPetService userPetService;
    private final UserService userService;
    private final ImageFileService imageFileService;
    private final ActivityHistoryMapper activityHistoryMapper;
    private final RoutePointService routePointService;

    /**
     * ?덈줈???쒕룞 湲곕줉???앹꽦?⑸땲??
     * <p>
     * <ol>
     * <li>?ъ슜??User) 諛?諛섎젮?숇Ъ(Pet) ?뺣낫瑜?議고쉶?⑸땲??</li>
     * <li>?붿껌???좎?媛 ?대떦 諛섎젮?숇Ъ???뱀씤??APPROVED) 二쇱씤?몄? 寃利앺빀?덈떎.</li>
     * <li>?대떦 諛섎젮?숇Ъ???대? 吏꾪뻾 以?IN_PROGRESS)?닿굅???湲?以?BEFORE)???쒕룞???덈뒗吏 ?뺤씤?섏뿬 以묐났 ?앹꽦??諛⑹??⑸땲??</li>
     * <li>寃利앹씠 ?꾨즺?섎㈃, ?쒕룞 ?곹깭瑜?'?쒖옉 ??BEFORE)'?쇰줈 ?ㅼ젙?섏뿬 DB????ν빀?덈떎.</li>
     * </ol>
     *
     * @param userId  ?붿껌???섑뻾?섎뒗 ?ъ슜?먯쓽 UUID
     * @param request ?앹꽦???쒕룞 ?뺣낫媛 ?닿릿 ?붿껌 DTO (petId, activityType)
     * @return ?앹꽦???쒕룞 湲곕줉??ID? ?좏삎???ы븿???묐떟 DTO
     * @throws ActivityHistoryException 沅뚰븳???녾굅??{@code CREATE_PERMISSION_DENIED}),
     * ?대? 吏꾪뻾 以?{@code ALREADY_IN_PROGRESS}) ?먮뒗 ?湲?以?{@code ALREADY_EXISTS_BEFORE})???쒕룞??議댁옱??寃쎌슦
     */
    @Transactional
    @Override
    public ActivityCreateResponse createActivity(
            UUID userId,
            ActivityCreateRequest request) {

        User user = userService.getUserById(userId);
        Pet pet = petService.getPetById(request.getPetId());

        boolean isOwner = userPetService.isApprovedPetOwner(user.getUsersId(), pet.getPetId());

        if (!isOwner) {
            throw new ActivityHistoryException(CREATE_PERMISSION_DENIED);
        }

        if (activityHistoryRepository.existsByPetAndActivityHistoryStatus(pet, ActivityHistoryStatus.IN_PROGRESS)) {
            throw new ActivityHistoryException(ALREADY_IN_PROGRESS);
        }

        if (activityHistoryRepository.existsByPetAndActivityHistoryStatus(pet, ActivityHistoryStatus.BEFORE)) {
            throw new ActivityHistoryException(ALREADY_EXISTS_BEFORE);
        }

        ActivityHistory activityHistory = request.toEntity(user, pet);
        ActivityHistory savedHistory = activityHistoryRepository.save(activityHistory);

        log.info("?쒕룞 湲곕줉 ?앹꽦 ?꾨즺 - HistoryId: {}, PetId: {}, User: {}",
                savedHistory.getHistoryId(), pet.getPetId(), userId);

        return ActivityCreateResponse.toDto(savedHistory, "?쒕룞 湲곕줉???깃났?곸쑝濡??앹꽦?섏뿀?듬땲??");
    }

    /**
     * ?쒕룞 湲곕줉???쒖옉(IN_PROGRESS)?섍굅?? 以묐떒???쒕룞???ш컻?⑸땲??
     * <p>
     * ?쒕룞???꾩옱 ?곹깭???곕씪 ??媛吏 濡쒖쭅?쇰줈 遺꾧린?⑸땲??
     * <ul>
     * <li><b>?쒖옉 ??BEFORE):</b> 理쒖큹 ?쒖옉?쇰줈 媛꾩＜?섏뿬 ?쒖옉 ?쒓컙怨??꾩튂 ?뺣낫瑜?湲곕줉?섍퀬 ?곹깭瑜?蹂寃쏀빀?덈떎.</li>
     * <li><b>痍⑥냼??CANCELED):</b> ?쒕룞 ?ш컻濡?媛꾩＜?섏뿬 ?곹깭瑜?蹂寃쏀븯怨?醫낅즺 ?쒓컙??珥덇린?뷀빀?덈떎. (湲곗〈 ?쒖옉 ?뺣낫 ?좎?)</li>
     * </ul>
     *
     * @param userId    ?붿껌???ъ슜?먯쓽 UUID
     * @param historyId ?쒕룞 湲곕줉 ID
     * @param request   ?쒖옉 ?쒖젏??GPS ?꾩튂 ?뺣낫(?꾨룄, 寃쎈룄)
     * @return 泥섎━ 寃곌낵 硫붿떆吏媛 ?닿릿 ?⑥닚 ?묐떟 DTO
     * @throws ActivityHistoryException
     * <ul>
     * <li>{@code HISTORY_NOT_FOUND}: ?대떦 ID???쒕룞 湲곕줉???녿뒗 寃쎌슦</li>
     * <li>{@code START_PERMISSION_DENIED}: ?쒕룞 湲곕줉???뚯쑀?먭? ?꾨땶 寃쎌슦</li>
     * <li>{@code ALREADY_IN_PROGRESS}: ?대? 吏꾪뻾 以묒씠嫄곕굹 醫낅즺???쒕룞??寃쎌슦</li>
     * </ul>
     */
    @Transactional
    @Override
    public ActivitySimpleResponse startActivity(UUID userId, Long historyId, ActivityStartRequest request) {

        ActivityHistory activityHistory = activityHistoryRepository.findById(historyId)
                .orElseThrow(() -> new ActivityHistoryException(HISTORY_NOT_FOUND));

        if (!activityHistory.getUser().getUsersId().equals(userId)) {
            throw new ActivityHistoryException(START_PERMISSION_DENIED);
        }

        ActivityHistoryStatus status = activityHistory.getActivityHistoryStatus();
        String message;

        if (status == ActivityHistoryStatus.BEFORE) {
            activityHistoryMapper.startActivity(
                    historyId,
                    ActivityHistoryStatus.IN_PROGRESS.name(),
                    request.getStartLatitude(),
                    request.getStartLongitude()
            );
            log.info("?쒕룞 理쒖큹 ?쒖옉 - HistoryId: {}, User: {}", historyId, userId);
            message = "?쒕룞 湲곕줉???쒖옉?섏뿀?듬땲??";

        } else if (status == ActivityHistoryStatus.CANCELED) {
            activityHistoryMapper.resumeActivity(
                    historyId,
                    ActivityHistoryStatus.IN_PROGRESS.name()
            );
            log.info("?쒕룞 ?ш컻 - HistoryId: {}, User: {}", historyId, userId);
            message = "?쒕룞 湲곕줉???ш컻?섏뿀?듬땲??";
        } else {
            throw new ActivityHistoryException(ALREADY_IN_PROGRESS);
        }

        return ActivitySimpleResponse.toDto(message);
    }

    /**
     * 吏꾪뻾 以묒씤 ?쒕룞??痍⑥냼(以묐떒) ?곹깭濡?蹂寃쏀빀?덈떎.
     * <p>
     * ?쒕룞 ?곹깭瑜?'痍⑥냼??CANCELED)'?쇰줈 蹂寃쏀븯怨? 以묐떒???쒖젏(醫낅즺 ?쒓컙)??湲곕줉?⑸땲??
     * </p>
     *
     * @param userId    ?붿껌???ъ슜?먯쓽 UUID
     * @param historyId ?쒕룞 湲곕줉 ID
     * @return 泥섎━ 寃곌낵 硫붿떆吏媛 ?닿릿 ?⑥닚 ?묐떟 DTO
     * @throws ActivityHistoryException
     * <ul>
     * <li>{@code HISTORY_NOT_FOUND}: ?대떦 ID???쒕룞 湲곕줉???녿뒗 寃쎌슦</li>
     * <li>{@code STOP_PERMISSION_DENIED}: ?쒕룞 湲곕줉???뚯쑀?먭? ?꾨땶 寃쎌슦</li>
     * <li>{@code ALREADY_COMPLETED}: 吏꾪뻾 以묒씤 ?쒕룞(IN_PROGRESS)???꾨땶 寃쎌슦</li>
     * </ul>
     */
    @Transactional
    @Override
    public ActivitySimpleResponse cancelActivity(UUID userId, Long historyId) {

        ActivityHistory activityHistory = activityHistoryRepository.findById(historyId)
                .orElseThrow(() -> new ActivityHistoryException(HISTORY_NOT_FOUND));

        if (!activityHistory.getUser().getUsersId().equals(userId)) {
            throw new ActivityHistoryException(STOP_PERMISSION_DENIED);
        }

        if (activityHistory.getActivityHistoryStatus() != ActivityHistoryStatus.IN_PROGRESS) {
            throw new ActivityHistoryException(ALREADY_COMPLETED);
        }

        activityHistoryMapper.cancelActivity(historyId, ActivityHistoryStatus.CANCELED.name());

        log.info("?쒕룞 以묐떒(痍⑥냼) ?꾨즺 - HistoryId: {}, User: {}", historyId, userId);

        return ActivitySimpleResponse.toDto("?쒕룞 湲곕줉???깃났?곸쑝濡?以묐떒?섏뿀?듬땲??");
    }

    /**
     * 吏꾪뻾 以묒씤 ?쒕룞???꾨즺(COMPLETED) ?곹깭濡?蹂寃쏀븯怨?醫낅즺 泥섎━瑜??섑뻾?⑸땲??
     * <p>
     * <ol>
     * <li>?쒕룞 湲곕줉 議댁옱 ?щ? 諛??붿껌??User)??沅뚰븳(?뚯쑀沅???寃利앺빀?덈떎.</li>
     * <li>?쒕룞 ?곹깭媛 '?쒖옉 ??BEFORE)'?닿굅???대? '醫낅즺(COMPLETED)'??寃쎌슦 ?덉쇅瑜?諛쒖깮?쒗궢?덈떎.</li>
     * <li>{@link RoutePointService}瑜??몄텧?섏뿬 珥??대룞 嫄곕━(Distance)瑜?怨꾩궛?⑸땲??</li>
     * <li>?쒕룞 ?곹깭瑜?'?꾨즺(COMPLETED)'濡?蹂寃쏀븯怨??쒕쾭 ?쒓컙(NOW)?쇰줈 醫낅즺 ?쒓컙??湲곕줉?⑸땲??</li>
     * <li>醫낅즺???쒕룞 ?뺣낫瑜??댁? ?묐떟 DTO瑜?諛섑솚?⑸땲??</li>
     * </ol>
     *
     * @param userId    ?붿껌???ъ슜?먯쓽 UUID
     * @param historyId ?쒕룞 湲곕줉 ID
     * @return 醫낅즺???쒕룞 湲곕줉???곸꽭 ?뺣낫 DTO (?대룞 嫄곕━ ?ы븿)
     */
    @Transactional
    @Override
    public ActivityFinishResponse finishActivity(UUID userId, Long historyId) {

        ActivityHistory activityHistory = activityHistoryRepository.findById(historyId)
                .orElseThrow(() -> new ActivityHistoryException(HISTORY_NOT_FOUND));

        if (!activityHistory.getUser().getUsersId().equals(userId)) {
            throw new ActivityHistoryException(STOP_PERMISSION_DENIED);
        }

        if (activityHistory.getActivityHistoryStatus() == ActivityHistoryStatus.BEFORE) {
            throw new ActivityHistoryException(ACTIVITY_NOT_STARTED);
        }

        if (activityHistory.getActivityHistoryStatus() != ActivityHistoryStatus.IN_PROGRESS) {
            throw new ActivityHistoryException(ALREADY_COMPLETED);
        }

        BigDecimal totalDistance = routePointService.calculateTotalDistance(historyId);

        LocalDateTime endTime = LocalDateTime.now();

        activityHistoryMapper.finishActivity(
                historyId,
                "COMPLETED",
                endTime,
                totalDistance
        );

        log.info("?쒕룞 醫낅즺 ?꾨즺 - HistoryId: {}, User: {}, Distance: {}m", historyId, userId, totalDistance);

        return ActivityFinishResponse.toDto(
                activityHistory.getHistoryId(),
                activityHistory.getActivityType(),
                totalDistance,
                activityHistory.getActivityHistoryStartAt(),
                endTime,
                "?쒕룞 湲곕줉???깃났?곸쑝濡?醫낅즺?섏뿀?듬땲??"
        );
    }

    /**
     * ?쒕룞 湲곕줉????젣?⑸땲??
     * <p>
     * ?쒕룞 湲곕줉 議댁옱 ?щ?? ?붿껌??User)???뚯쑀沅뚯쓣 寃利앺븳 ??
     * <b>JPA Repository</b>瑜??ъ슜?섏뿬 ?곗씠?곕? ??젣?⑸땲??
     * </p>
     *
     * @param userId    ?붿껌???ъ슜?먯쓽 UUID
     * @param historyId ??젣???쒕룞 湲곕줉 ID
     * @throws ActivityHistoryException
     * <ul>
     * <li>{@code HISTORY_NOT_FOUND}: ?대떦 ID???쒕룞 湲곕줉???녿뒗 寃쎌슦</li>
     * <li>{@code DELETE_PERMISSION_DENIED}: ?쒕룞 湲곕줉???뚯쑀?먭? ?꾨땶 寃쎌슦</li>
     * </ul>
     */
    @Transactional
    @Override
    public ActivitySimpleResponse deleteActivity(UUID userId, Long historyId) {

        ActivityHistory activityHistory = activityHistoryRepository.findById(historyId)
                .orElseThrow(() -> new ActivityHistoryException(HISTORY_NOT_FOUND));

        if (!activityHistory.getUser().getUsersId().equals(userId)) {
            throw new ActivityHistoryException(DELETE_PERMISSION_DENIED);
        }

        activityHistoryRepository.delete(activityHistory);

        log.info("?쒕룞 湲곕줉 ??젣 ?꾨즺 (JPA) - HistoryId: {}, User: {}", historyId, userId);

        return ActivitySimpleResponse.toDto("?쒕룞 湲곕줉???깃났?곸쑝濡???젣?섏뿀?듬땲??");
    }

    /**
     * ???쒕룞 湲곕줉??議고쉶?⑸땲?? (?섏씠吏?ㅼ씠??吏??
     * <p>
     * 1. ?ъ슜??ID濡??쒕룞 湲곕줉???섏씠吏?議고쉶?⑸땲?? (JPA媛 ?뺣젹 泥섎━)
     * 2. 議고쉶??湲곕줉?먯꽌 諛섎젮?숇Ъ ID瑜?異붿텧?섏뿬 ?꾨줈???대?吏瑜??쇨큵 議고쉶?⑸땲??(N+1 諛⑹?).
     * 3. ?뷀떚?곕? DTO濡?蹂?섑븯??諛섑솚?⑸땲??
     * </p>
     */
    @Transactional(readOnly = true)
    @Override
    public ActivityHistoryPageResponse getMyActivityHistory(UUID userId, Pageable pageable) {

        User user = userService.getUserById(userId);

        Page<ActivityHistory> historyPage = activityHistoryRepository.findAllByUser(user, pageable);

        List<Long> petIds = historyPage.getContent().stream()
                .map(history -> history.getPet().getPetId())
                .distinct()
                .toList();

        Map<Long, String> petImageMap = imageFileService.getProfileUrlsByPetIds(petIds);

        List<ActivityHistorySummary> summaries = historyPage.getContent().stream()
                .map(history -> {
                    String profileUrl = petImageMap.get(history.getPet().getPetId());
                    return ActivityHistorySummary.toDto(history, profileUrl);
                })
                .toList();

        return ActivityHistoryPageResponse.toDto(
                "?쒕룞 湲곕줉 紐⑸줉???깃났?곸쑝濡?議고쉶?덉뒿?덈떎.",
                historyPage,
                summaries
        );
    }

    /**
     * ?뱀젙 ?쒕룞 湲곕줉???곸꽭 ?뺣낫瑜?議고쉶?⑸땲??
     *
     * @param userId    ?붿껌???ъ슜?먯쓽 UUID
     * @param historyId 議고쉶???쒕룞 湲곕줉??ID
     * @return ?쒕룞 湲곕줉???곸꽭 ?뺣낫 DTO {@link ActivityHistoryDetailResponse}
     * @throws ActivityHistoryException
     * <ul>
     * <li>{@code HISTORY_NOT_FOUND}: ?대떦 ID???쒕룞 湲곕줉??議댁옱?섏? ?딅뒗 寃쎌슦</li>
     * <li>{@code VIEW_PERMISSION_DENIED}: ?붿껌?먭? ?대떦 諛섎젮?숇Ъ??媛議?援ъ꽦?먯씠 ?꾨땶 寃쎌슦</li>
     * <li>{@code ACTIVITY_NOT_STARTED}: ?쒕룞???꾩쭅 ?쒖옉?섏? ?딆? 寃쎌슦</li>
     * <li>{@code INVALID_REQUEST}: ?쒕룞??吏꾪뻾 以묒씠嫄곕굹 痍⑥냼???곹깭??寃쎌슦</li>
     * </ul>
     */
    @Transactional(readOnly = true)
    @Override
    public ActivityHistoryDetailResponse getActivityHistoryDetail(UUID userId, Long historyId) {
        ActivityHistory activityHistory = activityHistoryRepository.findById(historyId)
                .orElseThrow(() -> new ActivityHistoryException(HISTORY_NOT_FOUND));

        if (!userPetService.isApprovedPetOwner(userId, activityHistory.getPet().getPetId())) {
            throw new ActivityHistoryException(VIEW_PERMISSION_DENIED);
        }

        String status = activityHistory.getActivityHistoryStatus().name();

        if ("BEFORE".equals(status)) {
            throw new ActivityHistoryException(ACTIVITY_NOT_STARTED);
        }

        if ("IN_PROGRESS".equals(status) || "CANCELED".equals(status)) {
            throw new ActivityHistoryException(INVALID_REQUEST);
        }

        return ActivityHistoryDetailResponse.toDto(
                "?대떦 ?쒕룞 ?뺣낫瑜??깃났?곸쑝濡?議고쉶?덉뒿?덈떎.",
                activityHistory.getHistoryId(),
                activityHistory.getPet().getPetId(),
                activityHistory.getDistance(),
                activityHistory.getActivityHistoryStartAt(),
                activityHistory.getActivityHistoryEndAt(),
                activityHistory.getStartLatitude(),
                activityHistory.getStartLongitude(),
                0,
                false
        );
    }

    /**
     * ?뱀젙 諛섎젮?숇Ъ???꾩옱 ?쒕룞 ?곹깭瑜?議고쉶?⑸땲??
     * <p>
     * 1. 諛섎젮?숇Ъ 議댁옱 ?щ?? ?붿껌?먯쓽 議고쉶 沅뚰븳??寃利앺빀?덈떎.
     * 2. 媛??理쒓렐???쒕룞 湲곕줉??議고쉶?섏뿬 ?곹깭蹂꾨줈 硫붿떆吏? ID ?ы븿 ?щ?瑜?寃곗젙?⑸땲??
     * - IN_PROGRESS, BEFORE, CANCELED: ?≪뀡???꾩슂???곹깭?대?濡?historyId瑜?諛섑솚?⑸땲??
     * - COMPLETED: ?꾨즺???곹깭?대?濡?ID瑜?諛섑솚?섏? ?딆뒿?덈떎.
     * </p>
     *
     * @param userId ?붿껌???ъ슜?먯쓽 UUID
     * @param petId  ?곹깭瑜?議고쉶??諛섎젮?숇Ъ??ID
     * @return ?쒕룞 ?곹깭 ?묐떟 DTO {@link ActivityStatusResponse}
     */
    @Transactional(readOnly = true)
    @Override
    public ActivityStatusResponse getPetActivityStatus(UUID userId, Long petId) {

        Pet pet = petService.getPetById(petId);

        if (!userPetService.isApprovedPetOwner(userId, petId)) {
            throw new ActivityHistoryException(VIEW_PERMISSION_DENIED);
        }

        return buildActivityStatusResponse(pet);
    }

    /**
     * {@inheritDoc}
     * <p>
     * ?붾컮?댁뒪 ?좏겙 subject? 諛섎젮?숇Ъ ID??留ㅽ븨??寃利앺븳 ???쒕룞 ?곹깭瑜?議고쉶?⑸땲??
     */
    @Transactional(readOnly = true)
    @Override
    public ActivityStatusResponse getPetActivityStatusForDevice(String devicePrincipal, Long petId) {
        Pet pet = petService.getPetById(petId);

        if (!petService.isDevicePrincipalMatchedPet(devicePrincipal, petId)) {
            throw new ActivityHistoryException(VIEW_PERMISSION_DENIED);
        }

        return buildActivityStatusResponse(pet);
    }

    /**
     * ?뱀젙 ?쒕룞 湲곕줉???곸꽭 寃쎈줈(GPS 醫뚰몴 由ъ뒪??瑜?議고쉶?⑸땲??
     * <p>
     * 1. ?쒕룞 湲곕줉(History)??議댁옱 ?щ?瑜??뺤씤?⑸땲??
     * 2. ?붿껌???ъ슜??User)媛 ?대떦 諛섎젮?숇Ъ???뱀씤??蹂댄샇?먯씤吏 沅뚰븳??寃利앺빀?덈떎.
     * 3. RoutePointService瑜??듯빐 寃쎈줈 ?곗씠?곕? Map 由ъ뒪???뺥깭濡?議고쉶?⑸땲?? (Entity 吏곸젒 ?섏〈 ?쒓굅)
     * 4. 議고쉶???곗씠?곕? Response DTO濡?蹂?섑븯??諛섑솚?⑸땲??
     * </p>
     *
     * @param userId    ?붿껌???ъ슜?먯쓽 UUID
     * @param historyId 議고쉶???쒕룞 湲곕줉??ID
     * @return ?곸꽭 寃쎈줈 諛??쒕룞 ?뺣낫 ?묐떟 DTO {@link ActivityRouteResponse}
     * @throws ActivityHistoryException
     * <ul>
     * <li>{@code HISTORY_NOT_FOUND}: ?대떦 ID???쒕룞 湲곕줉??議댁옱?섏? ?딅뒗 寃쎌슦</li>
     * <li>{@code VIEW_PERMISSION_DENIED}: ?붿껌?먭? ?대떦 諛섎젮?숇Ъ??蹂댄샇?먭? ?꾨땶 寃쎌슦</li>
     * </ul>
     */
    @Transactional(readOnly = true)
    @Override
    public ActivityRouteResponse getActivityRoute(UUID userId, Long historyId) {

        ActivityHistory activityHistory = activityHistoryRepository.findById(historyId)
                .orElseThrow(() -> new ActivityHistoryException(HISTORY_NOT_FOUND));


        if (!userPetService.isApprovedPetOwner(userId, activityHistory.getPet().getPetId())) {
            throw new ActivityHistoryException(VIEW_PERMISSION_DENIED);
        }

        List<Map<String, Object>> routePointMaps = routePointService.getRoutePoints(historyId);

        List<RoutePointDto> routePointDtos = routePointMaps.stream()
                .map(map -> RoutePointDto.builder()
                        .routePointId((Long) map.get("routePointId"))
                        .latitude((BigDecimal) map.get("latitude"))
                        .longitude((BigDecimal) map.get("longitude"))
                        .measuredAt((LocalDateTime) map.get("measuredAt"))
                        .build())
                .toList();

        return ActivityRouteResponse.toDto(
                activityHistory,
                routePointDtos,
                "?곸꽭 寃쎈줈瑜?媛?몄삤?붾뜲 ?깃났?덉뒿?덈떎."
        );
    }

    /**
     * 嫄닿컯 遺꾩꽍 由ы룷???앹꽦???꾪빐 遺꾩꽍 ?⑥쐞蹂??쒕룞 湲곕줉??議고쉶?섍퀬 Map ?뺥깭濡?蹂?섑빀?덈떎.
     * <p>
     * 遺꾩꽍 ?⑥쐞???곕씪 ?쒕줈 ?ㅻⅨ Repository 硫붿꽌?쒕? ?몄텧?섎ŉ,
     * 諛섑솚 ???뷀떚?곕? ?몃?濡??몄텧?섏? ?딄퀬 ?꾩슂???꾨뱶留?異붿텧?⑸땲??
     * </p>
     *
     * @param petId         諛섎젮?숇Ъ ID
     * @param analysisType  遺꾩꽍 ?⑥쐞 (DAILY/WEEKLY/MONTHLY)
     * @param startDateTime 議고쉶 ?쒖옉 ?쒓컖 (?ы븿)
     * @param endDateTime   議고쉶 醫낅즺 ?쒓컖 (誘명룷???먮뒗 踰붿쐞 ?곹븳)
     * @return ?쒕룞 ?곗씠??紐⑸줉 (historyId, distance, startAt, endAt, status, activityType ??
     */
    @Transactional(readOnly = true)
    @Override
    public List<Map<String, Object>> getActivitiesForAnalysis(
            Long petId,
            String analysisType,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    ) {
        List<ActivityHistory> activityHistories;

        if ("DAILY".equals(analysisType)) {
            activityHistories = activityHistoryRepository.findAllByPet_PetIdAndActivityHistoryStartAtGreaterThanEqualAndActivityHistoryStartAtLessThanOrderByActivityHistoryStartAtAsc(
                    petId,
                    startDateTime,
                    endDateTime
            );
        } else if ("WEEKLY".equals(analysisType)) {
            activityHistories = activityHistoryRepository.findAllByPet_PetIdAndActivityHistoryStartAtGreaterThanEqualOrderByActivityHistoryStartAtAsc(
                    petId,
                    startDateTime
            );
        } else {
            activityHistories = activityHistoryRepository.findAllByPet_PetIdAndActivityHistoryStartAtBetweenOrderByActivityHistoryStartAtAsc(
                    petId,
                    startDateTime,
                    endDateTime
            );
        }

        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (ActivityHistory activityHistory : activityHistories) {
            Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("historyId", activityHistory.getHistoryId());
            row.put("distance", activityHistory.getDistance());
            row.put("startAt", activityHistory.getActivityHistoryStartAt());
            row.put("endAt", activityHistory.getActivityHistoryEndAt());
            row.put("startLatitude", activityHistory.getStartLatitude());
            row.put("startLongitude", activityHistory.getStartLongitude());
            row.put("status", activityHistory.getActivityHistoryStatus() == null ? null : activityHistory.getActivityHistoryStatus().name());
            row.put("activityType", activityHistory.getActivityType() == null ? null : activityHistory.getActivityType().name());
            result.add(row);
        }
        return result;
    }

    /**
     * ?뱀젙 諛섎젮?숇Ъ??理쒖떊 ?쒕룞 ?곹깭瑜?議고쉶???곹깭 硫붿떆吏? historyId瑜?議고빀???묐떟 DTO瑜??앹꽦?⑸땲??
     *
     * @param pet 議고쉶 ???諛섎젮?숇Ъ ?뷀떚??
     * @return ?쒕룞 ?곹깭 ?묐떟 DTO
     */
    private ActivityStatusResponse buildActivityStatusResponse(Pet pet) {
        return activityHistoryRepository.findFirstByPetOrderByHistoryIdDesc(pet)
                .map(history -> {
                    String status = history.getActivityHistoryStatus().name();

                    if ("IN_PROGRESS".equals(status)) {
                        return ActivityStatusResponse.toDto("?쒕룞 湲곕줉以묒씤 ?좎셿?숇Ъ?낅땲??", history.getHistoryId());
                    } else if ("BEFORE".equals(status)) {
                        return ActivityStatusResponse.toDto("?쒕룞 ?쒖옉 ???곹깭?낅땲??", history.getHistoryId());
                    } else if ("CANCELED".equals(status)) {
                        return ActivityStatusResponse.toDto("?쒕룞??以묐떒???곹깭?낅땲??", history.getHistoryId());
                    } else {
                        return ActivityStatusResponse.toDto("?꾩옱 吏꾪뻾 以묒씤 ?쒕룞???놁뒿?덈떎.", null);
                    }
                })
                .orElseGet(() -> ActivityStatusResponse.toDto("?쒕룞 湲곕줉???놁뒿?덈떎.", null));
    }

    /**
     * {@inheritDoc}
     * <p>
     * ?쒕룞 湲곕줉???곌껐??諛섎젮?숇Ъ ID濡??앹꽦???붾컮?댁뒪 UUID? ?꾩옱 Principal 媛믪쓣 鍮꾧탳?섏뿬 沅뚰븳??寃利앺빀?덈떎.
     */
    @Transactional(readOnly = true)
    @Override
    public boolean isDeviceAuthorizedForHistory(Long historyId, String devicePrincipal) {
        ActivityHistory activityHistory = activityHistoryRepository.findById(historyId)
                .orElseThrow(() -> new IllegalArgumentException("?대떦 ?쒕룞 湲곕줉??李얠쓣 ???놁뒿?덈떎."));

        Long petId = activityHistory.getPet().getPetId();
        String expectedDevicePrincipal = UUID.nameUUIDFromBytes(("DEVICE:" + petId).getBytes(StandardCharsets.UTF_8))
                .toString();

        return expectedDevicePrincipal.equals(devicePrincipal);
    }

    /**
     * {@inheritDoc}
     */
    @Transactional(readOnly = true)
    @Override
    public ActivityHistory getActivityHistoryById(Long historyId) {
        return activityHistoryRepository.findById(historyId)
                .orElseThrow(() -> new ActivityHistoryException(HISTORY_NOT_FOUND));
    }
}

