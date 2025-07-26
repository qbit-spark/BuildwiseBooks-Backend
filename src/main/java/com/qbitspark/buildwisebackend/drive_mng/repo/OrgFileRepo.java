package com.qbitspark.buildwisebackend.drive_mng.repo;

import com.qbitspark.buildwisebackend.drive_mng.entity.OrgFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrgFileRepo extends JpaRepository<OrgFileEntity, UUID> {
}
