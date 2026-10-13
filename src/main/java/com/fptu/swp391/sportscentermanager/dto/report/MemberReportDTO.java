package com.fptu.swp391.sportscentermanager.dto.report;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class MemberReportDTO {
    private long totalMembers;
    private long newMembersInRange;
    private long activeSubscriptionMembers;
    private long expiredOrNoSubMembers;
    private List<TimeSeriesDataPoint> growthTrend;
}
