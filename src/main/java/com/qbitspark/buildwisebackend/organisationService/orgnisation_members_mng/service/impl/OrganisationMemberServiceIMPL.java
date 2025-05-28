package com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.service.impl;

import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeauthentication.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.globeauthentication.entity.AccountEntity;
import com.qbitspark.buildwisebackend.organisationService.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisationService.organisation_mng.repo.OrganisationRepo;
import com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.entities.OrganisationInvitation;
import com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.enums.InvitationStatus;
import com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.enums.MemberRole;
import com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.repo.OrganisationInvitationRepo;
import com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.repo.OrganisationMemberRepo;
import com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.service.OrganisationMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganisationMemberServiceIMPL implements OrganisationMemberService {

    private final OrganisationMemberRepo organisationMemberRepo;
    private final AccountRepo accountRepo;
    private final OrganisationRepo organisationRepo;
    private final OrganisationInvitationRepo organisationInvitationRepo;

    @Transactional
    @Override
    public boolean inviteMember(UUID organisationId, String email, String role) throws ItemNotFoundException, AccessDeniedException {

            // Step 0: Check authenticated user permissions FIRST
            AccountEntity currentUser = getAuthenticatedAccount();

            // Step 1: Get the organisation
            OrganisationEntity organisation = organisationRepo.findById(organisationId)
                    .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

            // Step 2: Check if the current user can manage members in this org
            if (!canManageMembers(currentUser, organisation)) {
                throw new AccessDeniedException("You do not have permission to manage members in this organisation");
            }

            // Step 3: Check if email is already a member
            if (organisationMemberRepo.existsByAccountEmailAndOrganisation(email, organisation)) {
                return false; // Already a member
            }

            // Step 4: Check if email already has valid pending invitation
            if (hasValidPendingInvitation(email, organisation)) {
                return false; // Already has a valid invitation
            }

            // Step 5: Create invitation
            // TODO: Generate token, set fields, save invitation
            OrganisationInvitation invitation = new OrganisationInvitation();
            invitation.setOrganisation(organisation);
            invitation.setInviter(currentUser);
            invitation.setEmail(email);
            invitation.setRole(MemberRole.valueOf(role)); // Convert string to enum
            invitation.setToken(generateUniqueToken());
            invitation.setStatus(InvitationStatus.PENDING);
            invitation.setCreatedAt(LocalDateTime.now());
            invitation.setExpiresAt(LocalDateTime.now().plusDays(7)); // 7 days expiry

            return true;

    }

    @Override
    public boolean acceptInvitation(String token) {
        return false;
    }


    private AccountEntity getAuthenticatedAccount() throws ItemNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return extractAccount(authentication);
    }


    private boolean hasValidPendingInvitation(String email, OrganisationEntity organisation) {
        Optional<OrganisationInvitation> existingInvitation = organisationInvitationRepo
                .findByEmailAndOrganisationAndStatus(email, organisation, InvitationStatus.PENDING);

        if (existingInvitation.isPresent()) {
            OrganisationInvitation invitation = existingInvitation.get();

            if (invitation.getExpiresAt().isAfter(LocalDateTime.now())) {
                return true; // Valid pending invitation exists
            } else {
                // Mark as expired
                invitation.setStatus(InvitationStatus.EXPIRED);
                organisationInvitationRepo.save(invitation);
                return false; // No valid pending invitation
            }
        }

        return false; // No pending invitation
    }


    private boolean canManageMembers(AccountEntity user, OrganisationEntity organisation) {
        // Check if user is OWNER or ADMIN of this organisation
        Optional<OrganisationMember> membership = organisationMemberRepo
                .findByAccountAndOrganisation(user, organisation);

        if (membership.isPresent()) {
            MemberRole role = membership.get().getRole();
            return role == MemberRole.OWNER || role == MemberRole.ADMIN;
        }

        return false; // Not a member, can't manage
    }

    private String generateUniqueToken() {
        return UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
    }


    private AccountEntity extractAccount(Authentication authentication) throws ItemNotFoundException {
        if (authentication != null && authentication.isAuthenticated()) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userName = userDetails.getUsername();

            Optional<AccountEntity> userOptional = accountRepo.findByUserName(userName);
            if (userOptional.isPresent()) {
                return userOptional.get();
            } else {
                throw new ItemNotFoundException("User with given userName does not exist");
            }
        } else {
            throw new ItemNotFoundException("User is not authenticated");
        }
    }
}
