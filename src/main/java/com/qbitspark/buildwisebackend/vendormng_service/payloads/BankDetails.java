package com.qbitspark.buildwisebackend.vendormng_service.payloads;

import com.qbitspark.buildwisebackend.vendormng_service.enums.TanzaniaBank;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BankDetails {

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_branch", length = 100)
    private String bankBranch;

    @Column(name = "account_number", length = 20)
    private String accountNumber;

    @Column(name = "account_name", length = 100)
    private String accountName;

    @Column(name = "branch_code", length = 10)
    private String branchCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "bank_code", length = 20)
    private TanzaniaBank bankCode;

}