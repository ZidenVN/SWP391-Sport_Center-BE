package com.fptu.swp391.sportscentermanager.enums;

// BR-SUBJ-01: quyền vào khu tự tập của môn
public enum AreaAccess {
    OPEN,       // khu chung (Gym, Bơi...): chủ gói FREE hoặc CLASS của môn đều vào được
    CLASS_ONLY  // khu đặc thù (Boxing, MMA...): chỉ chủ gói CLASS của môn
}
