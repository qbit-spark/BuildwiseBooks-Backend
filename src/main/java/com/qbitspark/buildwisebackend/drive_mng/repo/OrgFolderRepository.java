package com.qbitspark.buildwisebackend.drive_mng.repo;

import com.qbitspark.buildwisebackend.drive_mng.entity.OrgFolderEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// OrgFolderRepository.java
public interface OrgFolderRepository extends JpaRepository<OrgFolderEntity, UUID> {

    OrgFolderEntity findByFolderNameAndOrganisationOrganisationIdAndParentFolderIsNull(String folderName, UUID organisationId);

    List<OrgFolderEntity> findByOrganisationOrganisationIdAndParentFolderIsNull(UUID organisationId);

    List<OrgFolderEntity> findByOrganisationOrganisationIdAndParentFolderFolderId(UUID organisationId, UUID parentFolderId);

    Optional<OrgFolderEntity> findByFolderNameAndOrganisationOrganisationIdAndParentFolderFolderId(
            String folderName, UUID organisationId, UUID parentFolderId);


    OrgFolderEntity findByFolderNameAndOrganisationAndParentFolder(String folderName, OrganisationEntity organisation, OrgFolderEntity parentFolder);

    OrgFolderEntity findByFolderNameAndOrganisationAndParentFolderIsNull(String folderName, OrganisationEntity organisation);
}