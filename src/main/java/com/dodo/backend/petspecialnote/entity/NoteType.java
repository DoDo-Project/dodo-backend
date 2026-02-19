package com.dodo.backend.petspecialnote.entity;

/**
 * 반려동물 특이사항의 분류 타입을 정의하는 열거형입니다.
 */
public enum NoteType {

    /**
     * 병원 관련 특이사항입니다.
     */
    HOSPITAL,

    /**
     * 약물 복용 관련 특이사항입니다.
     */
    MEDICATION,

    /**
     * 알레르기 관련 특이사항입니다.
     */
    ALLERGY,

    /**
     * 음식 관련 특이사항입니다.
     */
    FOOD,

    /**
     * 행동 관련 특이사항입니다.
     */
    BEHAVIOR,

    /**
     * 증상 관련 특이사항입니다.
     */
    SYMPTOM,

    /**
     * 기타 특이사항입니다.
     */
    ETC
}
