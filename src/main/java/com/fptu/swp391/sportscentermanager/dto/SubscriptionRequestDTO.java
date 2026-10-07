package com.fptu.swp391.sportscentermanager.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionRequestDTO {

    @NotNull
    private Long packageId;

    @NotNull
    @Pattern(regexp = "CASH|BANK_TRANSFER|CARD|E_WALLET",
        message = "paymentMethod phải là CASH, BANK_TRANSFER, CARD hoặc E_WALLET")
    private String paymentMethod;


}
