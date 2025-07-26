package com.qbitspark.buildwisebackend.drive_mng.enums;

import lombok.Getter;

@Getter
public enum SystemFileType {
    INVOICE("invoice"),
    VOUCHER("voucher"),
    BUDGET("budget"),
    VENDOR("others"),
    PAYMENT("payment"),
    GLOBAL("global_files");

    private final String folderName;

    SystemFileType(String folderName) {
        this.folderName = folderName;
    }

}