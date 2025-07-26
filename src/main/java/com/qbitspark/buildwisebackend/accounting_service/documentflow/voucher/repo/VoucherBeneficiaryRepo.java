package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.repo;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherBeneficiaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VoucherBeneficiaryRepo extends JpaRepository<VoucherBeneficiaryEntity, UUID> {
}
