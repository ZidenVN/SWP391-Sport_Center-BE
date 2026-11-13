package com.fptu.swp391.sportscentermanager.service.impl;

import com.fptu.swp391.sportscentermanager.dto.MemberProfileResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.WorkoutResultResponseDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AiChatServiceImplCoachPromptTest {

    @Test
    void coachPrompt_sendsTrainingDataButNoPersonalContact() {
        MemberProfileResponseDTO m = new MemberProfileResponseDTO();
        m.setFirstName("Nguyễn");
        m.setLastName("An");
        m.setEmail("an.nguyen@example.com");
        m.setPhone("0912345678");
        m.setGender("FEMALE");
        m.setTrainingGoal("Giảm cân, giảm mỡ");
        m.setHeightCm(160.0);
        m.setWeightKg(64.0);
        WorkoutResultResponseDTO r = new WorkoutResultResponseDTO();
        r.setMemberName("Nguyễn An");
        r.setActivityDetails("Squat 3x12");
        r.setCoachFeedback("Tốt");
        r.setLoggedDate(LocalDateTime.of(2026, 11, 10, 18, 0));

        String prompt = AiChatServiceImpl.coachPrompt(m, List.of(r));

        assertTrue(prompt.contains("Giảm cân, giảm mỡ"));
        assertTrue(prompt.contains("BMI 25.0"));
        assertTrue(prompt.contains("Squat 3x12"));
        for (String pii : List.of("an.nguyen@example.com", "0912345678", "Nguyễn")) {
            assertFalse(prompt.contains(pii), "prompt lộ dữ liệu cá nhân: " + pii);
        }
    }

    @Test
    void coachPrompt_newMemberWithoutHistory() {
        String prompt = AiChatServiceImpl.coachPrompt(new MemberProfileResponseDTO(), List.of());
        assertTrue(prompt.contains("Số buổi đã ghi kết quả: 0"));
        assertTrue(prompt.contains("Chưa có chiều cao/cân nặng"));
    }
}
