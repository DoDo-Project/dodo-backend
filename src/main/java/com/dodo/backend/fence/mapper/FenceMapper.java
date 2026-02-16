package com.dodo.backend.fence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 울타리(Fence) 도메인의 MyBatis 매퍼 인터페이스입니다.
 * <p>
 * 복잡한 조회/갱신 쿼리를 XML에 위임하기 위한 확장 지점으로 사용합니다.
 */
@Mapper
public interface FenceMapper {

    /**
     * 울타리 활성화 여부를 변경합니다.
     *
     * @param fenceId       울타리 ID
     * @param fenceIsActive 변경할 활성화 여부
     * @return 업데이트된 행 수
     */
    int updateFenceIsActive(@Param("fenceId") Long fenceId, @Param("fenceIsActive") Boolean fenceIsActive);

    /**
     * 울타리 범위 정보를 수정합니다.
     *
     * @param fenceId         울타리 ID
     * @param fenceName       울타리 이름
     * @param centerLatitude  울타리 중심 위도
     * @param centerLongitude 울타리 중심 경도
     * @param radius          울타리 반경(미터)
     * @return 업데이트된 행 수
     */
    int updateFenceRange(
            @Param("fenceId") Long fenceId,
            @Param("fenceName") String fenceName,
            @Param("centerLatitude") java.math.BigDecimal centerLatitude,
            @Param("centerLongitude") java.math.BigDecimal centerLongitude,
            @Param("radius") Integer radius
    );
}
