package com.qbitspark.buildwisebackend.projectmng_service.repo;

import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectTeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectTeamMemberRepo extends JpaRepository<ProjectTeamMember, Long> {
}