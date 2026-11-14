package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.PlanWorkflowDTO.*;

import java.time.ZoneId;
import java.util.List;

public interface PlanWorkflowService {
    ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    List<Assignment> list(Long userId, boolean coach);

    Assignment get(Long userId, boolean coach, Long id);

    Assignment submit(Long memberId, Long id, Submit input);

    Assignment review(Long coachId, Long id, Review input);
}
