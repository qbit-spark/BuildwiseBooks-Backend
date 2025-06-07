package com.qbitspark.buildwisebackend.projectmng_service.repo;

import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectTeamMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectTeamMemberRepo extends JpaRepository<ProjectTeamMemberEntity, UUID> {
    List<ProjectTeamMemberEntity> findByProjectProjectId(UUID projectId);
}