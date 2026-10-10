package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.dto.MemberSearchResponseDTO;

import java.util.List;

public interface ReceptionistService {

    List<MemberSearchResponseDTO> searchMembers(String keyword);
    MemberSearchResponseDTO getMember(Long memberId);

}
