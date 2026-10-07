package com.fptu.swp391.sportscentermanager.controller;


import com.fptu.swp391.sportscentermanager.dto.MemberProfileResponseDTO;
import com.fptu.swp391.sportscentermanager.dto.MemberProfileUpdateDTO;
import com.fptu.swp391.sportscentermanager.security.CustomUserDetails;
import com.fptu.swp391.sportscentermanager.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {
    //Controller gọi service
    private final MemberService memberService;

    //TODO: PreAuthorize có tác dụng gì? ND phải cung cấp trong ("") là gì?
    @PreAuthorize("hasRole('MEMBER') and hasAuthority('VIEW_OWN_PROFILE')")
    @GetMapping("/me")
    public ResponseEntity<MemberProfileResponseDTO> getMyProfile(
        @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(memberService.getProfile(principal.getUser().getUserId()));
    }

    @PreAuthorize("hasRole('MEMBER') and hasAuthority('UPDATE_OWN_PROFILE')")
    @PutMapping("/me")
    public ResponseEntity<MemberProfileResponseDTO> updateMyProfile(
        @AuthenticationPrincipal CustomUserDetails principal,
        @Valid @RequestBody MemberProfileUpdateDTO dto) {
        return ResponseEntity.ok(memberService.updateProfile(principal.getUser().getUserId(), dto));
    }


}
