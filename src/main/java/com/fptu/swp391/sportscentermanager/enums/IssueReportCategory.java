package com.fptu.swp391.sportscentermanager.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter  @RequiredArgsConstructor
public enum IssueReportCategory {
    MEMBER_FACILITY("MEMBER", IssueReportDesk.RECEPTION, "Cơ sở vật chất / thiết bị hỏng"),
    MEMBER_HYGIENE("MEMBER", IssueReportDesk.RECEPTION, "Vệ sinh"),
    MEMBER_COACH_CLASS("MEMBER", IssueReportDesk.MANAGER, "Phản ánh HLV / lớp"),
    MEMBER_PACKAGE_PAYMENT("MEMBER", IssueReportDesk.RECEPTION, "Gói tập / thanh toán"),
    MEMBER_OTHER("MEMBER", IssueReportDesk.RECEPTION, "Khác"),
    COACH_ROOM_EQUIPMENT("COACH", IssueReportDesk.MANAGER, "Thiết bị trong phòng hỏng"),
    COACH_SESSION_INCIDENT("COACH", IssueReportDesk.MANAGER, "Sự cố trong buổi học"),
    COACH_ROOM_ISSUE("COACH", IssueReportDesk.MANAGER, "Phòng học có vấn đề"),
    COACH_OTHER("COACH", IssueReportDesk.MANAGER, "Khác"),
    RECEPTION_FACILITY("RECEPTIONIST", IssueReportDesk.MANAGER, "Cơ sở vật chất"),
    RECEPTION_SYSTEM_PAYMENT("RECEPTIONIST", IssueReportDesk.MANAGER, "Lỗi hệ thống / thanh toán"),
    RECEPTION_MEMBER_VIOLATION("RECEPTIONIST", IssueReportDesk.MANAGER, "Hội viên vi phạm nội quy"),
    RECEPTION_OTHER("RECEPTIONIST", IssueReportDesk.MANAGER, "Khác")






    ;
    private final String role;
    private final IssueReportDesk desk;
    private final String label;

}
