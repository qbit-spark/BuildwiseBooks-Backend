package com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.repo;

import com.qbitspark.buildwisebackend.organisationService.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.entities.OrganisationInvitation;
import com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrganisationInvitationRepo extends JpaRepository<OrganisationInvitation, UUID> {
    Optional<OrganisationInvitation> findByToken(String token);
    boolean existsByEmailAndOrganisationAndStatus(String email, OrganisationEntity organisation, InvitationStatus status);

    Optional<OrganisationInvitation> findByEmailAndOrganisationAndStatus(String email, OrganisationEntity organisation, InvitationStatus status);

}
