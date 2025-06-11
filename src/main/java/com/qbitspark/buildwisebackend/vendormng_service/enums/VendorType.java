package com.qbitspark.buildwisebackend.vendormng_service.enums;

public enum VendorType {

    SUPPLIER("Supplier", "Provides materials, equipment, or goods"),
    CONTRACTOR("Contractor", "Provides construction or specialized labor services"),
    SUBCONTRACTOR("Subcontractor", "Works under main contractors for specific tasks"),
    CONSULTANT("Consultant", "Provides expert advice and professional services"),
    FREELANCER("Freelancer", "Independent service provider"),
    EMPLOYEE("Employee", "Company employee for expense reimbursements and petty cash"),
    EMPLOYEE_CONTRACTOR("Employee Contractor", "Current employee providing additional contract services"),
    SERVICE_PROVIDER("Service Provider", "Provides general services like cleaning, maintenance, security"),
    TECHNOLOGY_VENDOR("Technology Vendor", "Provides software, hardware, or IT services"),
    PROFESSIONAL_SERVICES("Professional Services", "Legal, accounting, financial, or other professional services"),
    LOGISTICS_PROVIDER("Logistics Provider", "Transportation, shipping, or delivery services"),
    MAINTENANCE_PROVIDER("Maintenance Provider", "Equipment maintenance and repair services"),
    TRAINING_PROVIDER("Training Provider", "Educational and training services"),
    OTHER("Other", "Other vendor types not specified above");

    private final String displayName;
    private final String description;

    VendorType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return displayName;
    }
}