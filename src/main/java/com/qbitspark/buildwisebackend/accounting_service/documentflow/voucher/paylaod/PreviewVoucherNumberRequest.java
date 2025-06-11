// PreviewVoucherNumberRequest.java
package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.paylaod;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreviewVoucherNumberRequest {

    @NotNull(message = "Project ID is required")
    private UUID projectId;
}