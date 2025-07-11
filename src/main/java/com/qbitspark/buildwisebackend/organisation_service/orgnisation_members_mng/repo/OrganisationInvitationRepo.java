package com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.repo;

import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationInvitation;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganisationInvitationRepo extends JpaRepository<OrganisationInvitation, UUID> {
    Optional<OrganisationInvitation> findByToken(String token);

    Optional<OrganisationInvitation> findByEmailAndOrganisationAndStatus(String email, OrganisationEntity organisation, InvitationStatus status);

    List<OrganisationInvitation> findAllByOrganisationAndStatus(OrganisationEntity organisation, InvitationStatus status);
    long countByOrganisationAndStatus(OrganisationEntity organisation, InvitationStatus status);

    Optional<OrganisationInvitation> findByOrganisationAndInvitationId(OrganisationEntity organisation, UUID invitationId);
}
