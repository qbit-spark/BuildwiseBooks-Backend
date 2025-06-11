package com.qbitspark.buildwisebackend.projectmng_service.repo;

import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectSubcontractorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectSubContractorRepo extends JpaRepository<ProjectSubcontractorEntity, UUID> {

    List<ProjectSubcontractorEntity> findByProjectProjectId(UUID projectId);

}
