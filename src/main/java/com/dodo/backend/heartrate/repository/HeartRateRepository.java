package com.dodo.backend.heartrate.repository;

import com.dodo.backend.heartrate.entity.HeartRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 심박수(HeartRate) 엔티티의 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 * <p>
 * Spring Data JPA의 {@link JpaRepository}를 상속받아 기본적인 CRUD(Create, Read, Update, Delete)
 * 및 페이징/정렬 기능을 제공합니다.
 */
@Repository
public interface HeartRateRepository extends JpaRepository<HeartRate, Long> {
}