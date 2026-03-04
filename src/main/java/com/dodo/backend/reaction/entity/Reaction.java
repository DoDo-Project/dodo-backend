package com.dodo.backend.reaction.entity;

import com.dodo.backend.activityhistory.entity.ActivityHistory;
import com.dodo.backend.board.entity.Board;
import com.dodo.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 게시글 및 활동 기록에 대한 반응 정보를 관리하는 엔티티입니다.
 * <p>
 * 데이터베이스 {@code reaction} 테이블과 매핑되며, 반응 주체 사용자와 대상 게시글,
 * 선택적으로 연결되는 활동 기록, 반응 유형 및 생성 시각을 포함합니다.
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "reaction")
public class Reaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reaction_id")
    private Long reactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "history_id")
    private ActivityHistory history;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private Board board;

    @CreatedDate
    @Column(name = "reaction_created_at", nullable = false, updatable = false)
    private LocalDateTime reactionCreatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_type", nullable = false)
    private ReactionType reactionType;
}
