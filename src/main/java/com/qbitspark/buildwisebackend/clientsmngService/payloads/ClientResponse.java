package com.qbitspark.buildwisebackend.clientsmngService.payloads;

import com.qbitspark.buildwisebackend.projectmngService.payloads.ProjectResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClientResponse {

    private UUID clientId;
    private String name;
    private String description;
    private String address;
    private String officePhone;
    private String tin;
    private String email;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer totalProjects;
    private List<ProjectResponse> projects;


}
