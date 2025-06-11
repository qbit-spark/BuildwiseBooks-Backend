package com.qbitspark.buildwisebackend.vendormng_service.enums;

public enum TanzaniaBank {
    CRDB("CRDB", "CRDB Bank PLC", "Cooperative Rural Development Bank"),
    NMB("NMB", "NMB Bank PLC", "National Microfinance Bank"),
    NBC("NBC", "NBC Bank (Tanzania) Limited", "National Bank of Commerce"),
    STANBIC("STANBIC", "Stanbic Bank Tanzania Limited", "Stanbic Bank"),
    STANDARD_CHARTERED("SCB", "Standard Chartered Bank (Tanzania) Limited", "Standard Chartered"),
    ABSA("ABSA", "Absa Bank Tanzania Limited", "Absa Bank (formerly Barclays)"),
    EXIM("EXIM", "Exim Bank (Tanzania) Limited", "Export Import Bank"),
    DTB("DTB", "Diamond Trust Bank Tanzania Limited", "Diamond Trust Bank"),
    EQUITY("EQUITY", "Equity Bank (Tanzania) Limited", "Equity Bank"),
    KCB("KCB", "KCB Bank Tanzania Limited", "Kenya Commercial Bank"),
    ACCESS("ACCESS", "Access Bank Tanzania Limited", "Access Bank"),
    UBA("UBA", "United Bank for Africa Tanzania Limited", "United Bank for Africa"),
    AZANIA("AZANIA", "Azania Bank Limited", "Azania Bank"),
    COMSIP("COMSIP", "Commercial Bank of Africa Tanzania Limited", "Commercial Bank of Africa"),
    FBME("FBME", "FBME Bank Limited", "FBME Bank"),
    HABIB("HABIB", "Habib African Bank Limited", "Habib African Bank"),
    I_M("IM", "I&M Bank (Tanzania) Limited", "Investment and Mortgages Bank"),
    MUCOBA("MUCOBA", "Mufindi Community Bank Limited", "Mufindi Community Bank"),
    PEOPLES("PEOPLES", "Peoples Bank of Zanzibar Limited", "Peoples Bank of Zanzibar"),
    PRIDE("PRIDE", "Pride Microfinance Bank Limited", "Pride Microfinance"),
    SAVINGS("SAVINGS", "Savings and Finance Commercial Bank Limited", "Savings and Finance Bank"),
    TPB("TPB", "Tanzania Postal Bank", "Tanzania Postal Bank"),
    TIB("TIB", "Tanzania Investment Bank", "Tanzania Investment Bank"),
    BOT("BOT", "Bank of Tanzania", "Central Bank of Tanzania"),
    OTHER("OTHER", "Other Bank", "Other banking institution");

    private final String code;
    private final String fullName;
    private final String displayName;

    TanzaniaBank(String code, String fullName, String displayName) {
        this.code = code;
        this.fullName = fullName;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getFullName() {
        return fullName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    // Utility method to find bank by code
    public static TanzaniaBank findByCode(String code) {
        for (TanzaniaBank bank : values()) {
            if (bank.getCode().equalsIgnoreCase(code)) {
                return bank;
            }
        }
        return OTHER;
    }
}