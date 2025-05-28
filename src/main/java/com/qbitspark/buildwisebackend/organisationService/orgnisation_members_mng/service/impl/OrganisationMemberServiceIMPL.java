package com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.service.impl;

import com.qbitspark.buildwisebackend.emails.service.GlobeMailService;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.InvitationAlreadyProcessedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.InvitationExpiredException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeauthentication.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.globeauthentication.entity.AccountEntity;
import com.qbitspark.buildwisebackend.organisationService.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisationService.organisation_mng.repo.OrganisationRepo;
import com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.entities.OrganisationInvitation;
import com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.enums.InvitationStatus;
import com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.enums.MemberRole;
import com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.enums.MemberStatus;
import com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.payloads.InvitationInfoResponse;
import com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.repo.OrganisationInvitationRepo;
import com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.repo.OrganisationMemberRepo;
import com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.service.OrganisationMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
    private final GlobeMailService globeMailService;

    @Value("${frontend.base-url}")
    private String frontendBaseUrl;

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

        // Save invitation to a database
        OrganisationInvitation savedInvitation = organisationInvitationRepo.save(invitation);

        // Step 6: Send invitation email
        if (sendInvitationEmail(savedInvitation)) {
            return true; // Success - transaction commits
        } else {
            return false; // Email failed - transaction rolls back automatically
        }

    }


    @Override
    public void addOwnerAsMember(OrganisationEntity organisation, AccountEntity owner) {
        // Check if an owner is already a member (safety check)
        Optional<OrganisationMember> existingMember = organisationMemberRepo
                .findByAccountAndOrganisation(owner, organisation);

        if (existingMember.isEmpty()) {
            OrganisationMember ownerMember = new OrganisationMember();
            ownerMember.setOrganisation(organisation);
            ownerMember.setAccount(owner);
            ownerMember.setRole(MemberRole.OWNER);
            ownerMember.setStatus(MemberStatus.ACTIVE);
            ownerMember.setJoinedAt(LocalDateTime.now());
            ownerMember.setInvitedBy(owner.getId());

            organisationMemberRepo.save(ownerMember);
        }
    }


    @Transactional
    @Override
    public boolean acceptInvitation(String token) throws ItemNotFoundException, InvitationAlreadyProcessedException, InvitationExpiredException {

        // Step 1: Find an invitation by token
        OrganisationInvitation invitation = organisationInvitationRepo.findByToken(token)
                .orElseThrow(() -> new ItemNotFoundException("Invalid invitation token"));

        // Step 2: Validate invitation (throws exceptions if invalid)
        validateInvitation(invitation);

        // Step 3: Check if the user already exists and handle verification
        Optional<AccountEntity> existingUser = accountRepo.findByEmail(invitation.getEmail());

        if (existingUser.isEmpty()) {
            // User doesn't exist - they need to register first
            throw new ItemNotFoundException("Account not found. Please register ");
        }

        AccountEntity user = existingUser.get();

        // Step 3.1: Check if a user account is verified
        if (!user.getIsVerified()) {
            throw new InvitationAlreadyProcessedException("Please verify your email address first before accepting this invitation");
        }

        // Step 3.2: Check if the user email is verified (additional safety check)
        if (!user.getIsEmailVerified()) {
            throw new InvitationAlreadyProcessedException("Please verify your email address first before accepting this invitation");
        }

        // Step 4: Check if a user is already a member
        Optional<OrganisationMember> existingMember = organisationMemberRepo.findByAccountAndOrganisation(user, invitation.getOrganisation());
        if (existingMember.isPresent()) {
            // User is already a member, mark the invitation as accepted anyway
            invitation.setStatus(InvitationStatus.ACCEPTED);
            invitation.setRespondedAt(LocalDateTime.now());
            organisationInvitationRepo.save(invitation);
            throw new InvitationAlreadyProcessedException("You are already a member of this organisation");
        }

        // Step 5: Create membership
        OrganisationMember member = new OrganisationMember();
        member.setOrganisation(invitation.getOrganisation());
        member.setAccount(user);
        member.setRole(invitation.getRole());
        member.setStatus(MemberStatus.ACTIVE);
        member.setJoinedAt(LocalDateTime.now());
        member.setInvitedBy(invitation.getInviter().getId());

        organisationMemberRepo.save(member);

        // Step 6: Update invitation status
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setRespondedAt(LocalDateTime.now());
        organisationInvitationRepo.save(invitation);

        return true;
    }


    @Transactional
    @Override
    public boolean declineInvitation(String token) throws ItemNotFoundException, InvitationAlreadyProcessedException, InvitationExpiredException {

        // Step 1: Find invitation by token
        OrganisationInvitation invitation = organisationInvitationRepo.findByToken(token)
                .orElseThrow(() -> new ItemNotFoundException("Invalid invitation token"));

        // Step 2: Validate invitation (throws exceptions if invalid)
        validateInvitation(invitation);

        // Step 3: Mark as declined
        invitation.setStatus(InvitationStatus.DECLINED);
        invitation.setRespondedAt(LocalDateTime.now());
        organisationInvitationRepo.save(invitation);

        return true;
    }

    @Override
    public InvitationInfoResponse getInvitationInfo(String token) throws ItemNotFoundException {

        // Step 1: Find invitation by token
        OrganisationInvitation invitation = organisationInvitationRepo.findByToken(token)
                .orElseThrow(() -> new ItemNotFoundException("Invalid invitation token"));

        // Step 2: Check if invitation is expired (but don't update status here)
        boolean isExpired = invitation.getExpiresAt().isBefore(LocalDateTime.now());

        // Step 3: Build safe response
        InvitationInfoResponse response = new InvitationInfoResponse();
        response.setOrganisationName(invitation.getOrganisation().getOrganisationName());
        response.setOrganisationDescription(invitation.getOrganisation().getOrganisationDescription());
        response.setInviterName(invitation.getInviter().getUserName()); // Only username, no sensitive info
        response.setRole(invitation.getRole().toString());
        response.setInvitedEmail(invitation.getEmail());
        response.setInvitedAt(invitation.getCreatedAt());
        response.setExpiresAt(invitation.getExpiresAt());
        response.setStatus(invitation.getStatus().toString());
        response.setExpired(isExpired);

        // Step 4: Determine what actions are available
        boolean isPending = invitation.getStatus() == InvitationStatus.PENDING;
        response.setCanAccept(isPending && !isExpired);
        response.setCanDecline(isPending && !isExpired);

        return response;
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

    private void validateInvitation(OrganisationInvitation invitation) throws InvitationExpiredException, InvitationAlreadyProcessedException {

        // First check if invitation is expired (regardless of status)
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            // Update status to expired if it's still pending
            if (invitation.getStatus() == InvitationStatus.PENDING) {
                invitation.setStatus(InvitationStatus.EXPIRED);
                organisationInvitationRepo.save(invitation);
            }
            throw new InvitationExpiredException("This invitation has expired");
        }

        // Then check if the invitation has already been processed
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            String message = switch (invitation.getStatus()) {
                case ACCEPTED -> "This invitation has already been accepted";
                case DECLINED -> "This invitation has already been declined";
                case EXPIRED -> "This invitation has expired";
                case REVOKED -> "This invitation has been revoked";
                case PENDING -> "This invitation is still pending"; // This shouldn't happen due to our check above
                default -> "This invitation cannot be processed";
            };
            throw new InvitationAlreadyProcessedException(message);
        }
    }

    private String generateUniqueToken() {
        return UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
    }

    private boolean sendInvitationEmail(OrganisationInvitation invitation) {
        try {
            String acceptLink = frontendBaseUrl + "/invitation/accept?token=" + invitation.getToken();
            String declineLink = frontendBaseUrl + "/invitation/decline?token=" + invitation.getToken();

            return globeMailService.sendOrganisationInvitationEmail(
                    invitation.getEmail(),
                    invitation.getOrganisation().getOrganisationName(),
                    invitation.getInviter().getUserName(),
                    invitation.getRole().toString(),
                    acceptLink,
                    declineLink
            );
        } catch (Exception e) {
            return false;
        }
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
