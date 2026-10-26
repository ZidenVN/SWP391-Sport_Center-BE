package com.fptu.swp391.sportscentermanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import com.fptu.swp391.sportscentermanager.enums.AreaAccess;
@Getter
@Setter
public class SubjectRequestDTO {
    @NotBlank
    private String subjectName;

    @Size(max = 500)
    private String description;
    // OPEN | CLASS_ONLY. Bỏ trống: tạo mới = OPEN, cập nhật = giữ nguyên
    private AreaAccess areaAccess;
}
