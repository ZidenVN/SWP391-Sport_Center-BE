package com.fptu.swp391.sportscentermanager.repository;

import com.fptu.swp391.sportscentermanager.entity.MemberQuizAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface MemberQuizAnswerRepository extends JpaRepository<MemberQuizAnswer, Long> {
    @Modifying
    @Query("delete from MemberQuizAnswer a where a.member.userId = :memberId")
    void deleteByMemberId(@Param("memberId") Long memberId);

    boolean existsByQuestion_QuestionId(Long questionId);

    boolean existsByOption_OptionId(Long optionId);

    // Các option của 1 câu đã có hội viên chọn (sửa câu hỏi: option này chỉ được ẩn, không xoá)
    @Query("select distinct a.option.optionId from MemberQuizAnswer a where a.question.questionId = :questionId and a.option is not null")
    Set<Long> findAnsweredOptionIds(@Param("questionId") Long questionId);
}
