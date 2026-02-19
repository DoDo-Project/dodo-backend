package com.dodo.backend.petspecialnote.entity;

import com.dodo.backend.pet.entity.Pet;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 반려동물의 특이사항 정보를 저장하는 엔티티입니다.
 * <p>
 * 데이터베이스의 {@code pet_special_notes} 테이블과 매핑되며,
 * 반려동물별 메모성 특이사항을 유형과 함께 관리합니다.
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "pet_special_notes")
public class PetSpecialNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "note_id")
    private Long noteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @Column(name = "note_content", nullable = false, columnDefinition = "TEXT")
    private String noteContent;

    @CreatedDate
    @Column(name = "pet_special_notes_created_at", nullable = false, updatable = false)
    private LocalDateTime petSpecialNotesCreatedAt;

    @LastModifiedDate
    @Column(name = "pet_special_notes_updated_at")
    private LocalDateTime petSpecialNotesUpdatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "note_type", nullable = false)
    private NoteType noteType;
}
