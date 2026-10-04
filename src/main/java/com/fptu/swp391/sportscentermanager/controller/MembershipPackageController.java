package com.fptu.swp391.sportscentermanager.controller;

import com.fptu.swp391.sportscentermanager.entity.MembershipPackage;
import com.fptu.swp391.sportscentermanager.service.MembershipPackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/packages")
@RequiredArgsConstructor
public class MembershipPackageController {
    private final MembershipPackageService packageService;

    @GetMapping
    public ResponseEntity<List<MembershipPackage>> getAllPackage() {
        return ResponseEntity.ok(packageService.getAllPackage());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MembershipPackage> getPackageById(@PathVariable Long id) {
        return ResponseEntity.ok(packageService.getPackageById(id));
    }

    @PreAuthorize("hasAuthority('MANAGE_PACKAGE')")
    @PostMapping
    public ResponseEntity<MembershipPackage> createPackage(@RequestBody MembershipPackage membershipPackage) {
        return ResponseEntity.ok(packageService.createPackage(membershipPackage));
    }

    @PreAuthorize("hasAuthority('MANAGE_PACKAGE')")
    @PutMapping("/{id}")
    public ResponseEntity<MembershipPackage> updatePackage(@PathVariable Long id, @RequestBody MembershipPackage membershipPackage) {
        return ResponseEntity.ok(packageService.updatePackage(id, membershipPackage));
    }

    @PreAuthorize("hasAuthority('MANAGE_PACKAGE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePackage(@PathVariable Long id) {
        packageService.deletePackage(id);
        return ResponseEntity.ok("Đã xóa thành công gói đăng ký!");
    }
}
