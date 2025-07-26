package com.qbitspark.buildwisebackend.drive_mng.enums;

import lombok.Getter;

@Getter
public enum SystemFileType {
    INVOICES("invoices"),
    VOUCHERS("vouchers"),
    BUDGETS("budgets"),
    PAYMENTS("payments"),
    OTHERS("Others"),
    VENDORS("vendors"),
    GLOBAL("global_files");

    private final String folderName;

    SystemFileType(String folderName) {
        this.folderName = folderName;
    }

}