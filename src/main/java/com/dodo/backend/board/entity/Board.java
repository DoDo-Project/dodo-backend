package com.dodo.backend.board.entity;

import com.dodo.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 게시글(Board) 정보를 관리하는 엔티티입니다.
 * <p>
 * 데이터베이스 {@code board} 테이블과 매핑되며, 작성자, 제목/본문, 조회수,
 * 게시글 상태 및 게시판 유형 정보를 포함합니다.
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "board")
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_id")
    private Long boardId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreatedDate
    @Column(name = "board_created_at", nullable = false, updatable = false)
    private LocalDateTime boardCreatedAt;

    @Column(name = "board_title", length = 255, nullable = false)
    private String boardTitle;

    @Column(name = "board_content", columnDefinition = "TEXT", nullable = false)
    private String boardContent;

    @Builder.Default
    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    @LastModifiedDate
    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "board_status", nullable = false)
    private BoardStatus boardStatus;

    @Column(name = "board_status_updated_at")
    private LocalDateTime boardStatusUpdatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "board_type", nullable = false)
    private BoardType boardType;

    /**
     * 영속화 직전에 기본값이 누락된 필드를 보정합니다.
     */
    @PrePersist
    private void prePersist() {
        if (viewCount == null) {
            viewCount = 0;
        }

        if (boardStatusUpdatedAt == null) {
            boardStatusUpdatedAt = LocalDateTime.now();
        }
    }
}
