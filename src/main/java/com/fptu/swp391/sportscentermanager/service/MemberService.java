package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.MemberProfileResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.MemberProfileUpdateDTO;

public interface MemberService {

    MemberProfileResponseDTO getProfile(Long memberID);
    MemberProfileResponseDTO updateProfile(Long memberID, MemberProfileUpdateDTO dto);

}
