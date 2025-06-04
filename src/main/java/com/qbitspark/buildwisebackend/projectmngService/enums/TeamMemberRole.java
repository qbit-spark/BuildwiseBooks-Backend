package com.qbitspark.buildwisebackend.projectmngService.enums;
import lombok.Getter;

@Getter
public enum TeamMemberRole {
    ENGINEER("Engineer"),
    ARCHITECT("Architect"),
    PROJECT_MANAGER("Project Manager"),
    LEAD_CONSULTANT("Lead Consultant"),
    MEMBER("Member"),
    DEVELOPER("Developer"),
    OWNER("Owner"),
    ACCOUNTANT("Accountant");

    private final String displayName;

    TeamMemberRole(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return this.displayName;
    }

    public static TeamMemberRole fromString(String text) {
        for (TeamMemberRole role : TeamMemberRole.values()) {
            if (role.displayName.equalsIgnoreCase(text) || role.name().equalsIgnoreCase(text)) {
                return role;
            }
        }
        throw new IllegalArgumentException("No enum constant for: " + text);
    }
}
