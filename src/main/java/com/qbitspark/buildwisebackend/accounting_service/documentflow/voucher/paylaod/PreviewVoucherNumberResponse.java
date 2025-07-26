// PreviewVoucherNumberResponse.java
package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.paylaod;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreviewVoucherNumberResponse {

    private String nextVoucherNumber;
    private String projectCode;
    private String projectName;
    private String organisationName;
}