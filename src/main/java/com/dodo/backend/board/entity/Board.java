package com.dodo.backend.board.entity;

import com.dodo.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 게시글(Board) 정보를 관리하는 엔티티입니다.
 * <p>
 * 데이터베이스 {@code board} 테이블과 매핑되며, 작성자, 제목/본문, 조회 수,
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
    @ColumnDefault("0")
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

    @Enumerated(EnumType.STRING)
    @Column(name = "notice_tag")
    private NoticeTag noticeTag;

    /**
     * 공지 게시글 내용을 수정합니다.
     *
     * @param boardTitle 수정할 공지 제목
     * @param boardContent 수정할 공지 내용
     */
    public void updateAnnouncement(String boardTitle, String boardContent) {
        if (boardTitle != null) {
            this.boardTitle = boardTitle;
        }
        if (boardContent != null) {
            this.boardContent = boardContent;
        }
    }

    /**
     * 게시글 상태를 변경합니다.
     *
     * @param boardStatus 변경할 게시글 상태
     */
    public void updateBoardStatus(BoardStatus boardStatus) {
        this.boardStatus = boardStatus;
        this.boardStatusUpdatedAt = LocalDateTime.now();
    }
}
