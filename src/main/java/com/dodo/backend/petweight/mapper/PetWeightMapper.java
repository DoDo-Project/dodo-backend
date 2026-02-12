package com.dodo.backend.petweight.mapper;

import com.dodo.backend.petweight.dto.request.PetWeightRequest.PetWeightUpdateRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 반려동물 체중(PetWeight) 데이터의 MyBatis 매퍼 인터페이스입니다.
 * <p>
 * 복잡한 동적 쿼리나 부분 업데이트(Patch) 로직을 처리합니다.
 */
@Mapper
public interface PetWeightMapper {

    /**
     * 반려동물의 체중 기록을 선택적으로 업데이트합니다.
     * <p>
     * DTO의 필드 값이 null이 아닌 경우에만 해당 컬럼을 수정합니다 (Dynamic Update).
     *
     * @param weightId 수정할 체중 기록의 고유 식별자
     * @param request  변경할 데이터(몸무게, 날짜)가 담긴 DTO
     */
    void updatePetWeight(@Param("weightId") Long weightId, @Param("request") PetWeightUpdateRequest request);
}