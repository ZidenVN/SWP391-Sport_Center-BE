package com.fptu.swp391.sportscentermanager.dto;

import com.fptu.swp391.sportscentermanager.enums.AreaAccess;
import lombok.*;

// Môn của gói + quyền vào khu (BR-CHK-02: lễ tân biết member được vào khu nào)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectAccessDTO {
    private Long subjectId;
    private String subjectName;
    private AreaAccess areaAccess;
}
