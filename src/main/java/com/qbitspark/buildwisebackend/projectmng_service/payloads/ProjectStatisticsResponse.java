package com.qbitspark.buildwisebackend.projectmng_service.payloads;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectStatisticsResponse {
    private long totalProjects;
    private long activeProjects;
    private long completedProjects;
    private long pausedProjects;
    private long cancelledProjects;
    private BigDecimal totalBudget;
    private BigDecimal averageBudget;
    private int totalTeamMembers;
    private double averageTeamSize;
}
