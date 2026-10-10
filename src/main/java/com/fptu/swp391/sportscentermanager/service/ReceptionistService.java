package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.MemberSearchResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.WalkInMemberRequestDTO;
import com.fptu.swp391.sportscentermanager.dto.WalkInMemberResponseDTO;

import java.util.List;

public interface ReceptionistService {

    List<MemberSearchResponseDTO> searchMembers(String keyword);
    MemberSearchResponseDTO getMember(Long memberId);
    WalkInMemberResponseDTO registerWalkInMember(WalkInMemberRequestDTO dto);
}
