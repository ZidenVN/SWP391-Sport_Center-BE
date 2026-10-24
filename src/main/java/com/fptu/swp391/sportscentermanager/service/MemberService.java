package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.MemberAttendanceResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.MemberProfileResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.MemberProfileUpdateDTO;

import java.util.List;

public interface MemberService {

    MemberProfileResponseDTO getProfile(Long memberID);
    MemberProfileResponseDTO updateProfile(Long memberID, MemberProfileUpdateDTO dto);

    List<MemberAttendanceResponseDTO> getMyAttendances(Long memberId);

}
