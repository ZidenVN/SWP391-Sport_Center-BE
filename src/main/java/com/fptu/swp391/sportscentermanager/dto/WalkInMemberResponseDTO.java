package com.fptu.swp391.sportscentermanager.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalkInMemberResponseDTO {
    private MemberSearchResponseDTO member;
    // Mật khẩu tạm, chỉ trả về 1 lần duy nhất để lễ tân đưa cho khách
    private String temporaryPassword;
}
