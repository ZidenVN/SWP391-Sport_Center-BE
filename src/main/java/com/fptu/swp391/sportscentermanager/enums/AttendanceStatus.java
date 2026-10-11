package com.fptu.swp391.sportscentermanager.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AttendanceStatus {
    PRESENT("Có mặt"),
    ABSENT("Vắng mặt"),
    LATE("Đến muộn"),
    ON_LEAVE("Nghỉ phép");
    private final String description;
}
