package com.dodo.backend.imagefile.entity;

import com.dodo.backend.board.entity.Board;
import com.dodo.backend.pet.entity.Pet;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 이미지 파일 정보를 관리하는 엔티티입니다.
 * <p>
 * {@link Pet} 프로필 이미지 또는 {@link Board} 게시글 이미지와 연결되며,
 * 파일의 메타데이터(크기, 원본명 등)와 저장된 URL 경로를 포함합니다.
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "image_file")
public class ImageFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_file_id")
    private Long imageFileId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = true)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = true)
    private Board board;

    @Column(name = "image_file_url", length = 2048, nullable = false)
    private String imageFileUrl;

    @Column(name = "size", nullable = false)
    private Long size;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @CreatedDate
    @Column(name = "image_file_created_at", nullable = false, updatable = false)
    private LocalDateTime imageFileCreatedAt;
}
