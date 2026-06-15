package com.dodo.backend.report.entity;

/**
 * 신고 사유를 정의하는 열거형입니다.
 */
public enum ReportReason {
    /**
     * 스팸 또는 광고성 게시물입니다.
     */
    SPAM,

    /**
     * 음란물 또는 선정적 콘텐츠입니다.
     */
    OBSCENITY,

    /**
     * 욕설, 비방, 혐오 발언입니다.
     */
    ABUSE,

    /**
     * 부적절한 프로필입니다.
     */
    INAPPROPRIATE_PROFILE,

    /**
     * 스팸 또는 광고 목적의 계정/댓글입니다.
     */
    SPAM_ADVERTISING,

    /**
     * 사칭 또는 명의 도용입니다.
     */
    IMPERSONATION,

    /**
     * 혐오 발언입니다.
     */
    HATE_SPEECH,

    /**
     * 개인정보 노출입니다.
     */
    PRIVATE_INFO_EXPOSURE,

    /**
     * 괴롭힘 또는 지속적인 공격입니다.
     */
    HARASSMENT
}
