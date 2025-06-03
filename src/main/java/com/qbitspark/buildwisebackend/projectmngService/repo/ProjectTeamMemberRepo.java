package com.qbitspark.buildwisebackend.projectmngService.repo;

import com.qbitspark.buildwisebackend.projectmngService.entity.ProjectTeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectTeamMemberRepo extends JpaRepository<ProjectTeamMember, Long> {
}