package com.qbitspark.buildwisebackend.projectmng_service.payloads;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;


@Data
public class AvailableTeamMemberResponse {
    private UUID memberId;
    private String userName;
    private String email;
    private LocalDateTime joinedAt;
}
