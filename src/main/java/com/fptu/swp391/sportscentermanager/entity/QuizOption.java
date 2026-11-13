package com.fptu.swp391.sportscentermanager.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "quiz_options")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizOption {
    // Lựa chọn "Khác" của câu TRAINING_GOAL: chọn thì hội viên tự nhập mục tiêu
    public static final String OTHER_CODE = "OTHER";
    public static final int OTHER_TEXT_MAX = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_id")
    private Long optionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name="code", length = 50)
    private String code;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "ACTIVE";
}
