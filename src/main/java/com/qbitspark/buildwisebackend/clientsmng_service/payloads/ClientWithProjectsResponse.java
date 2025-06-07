package com.qbitspark.buildwisebackend.clientsmng_service.payloads;

import com.qbitspark.buildwisebackend.projectmngService.payloads.ProjectResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientWithProjectsResponse {
    private ClientResponse client;
    private List<ProjectResponse> projects;
    private int totalProjects;
}
