package com.fptu.swp391.sportscentermanager.enums;

public enum IssueReportStatus {
    OPEN("Mới gửi"),
    IN_PROGRESS("Đang xử lý"),
    RESOLVED("Đã xử lý"),
    REJECTED("Từ chối"),
    CANCELLED("Đã rút");

    private final String label;

    IssueReportStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
