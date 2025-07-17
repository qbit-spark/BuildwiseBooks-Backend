package com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.service.impl;
import com.qbitspark.buildwisebackend.emails_service.GlobeMailService;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.*;
import com.qbitspark.buildwisebackend.authentication_service.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.repo.OrganisationRepo;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationInvitation;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.InvitationStatus;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberStatus;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.payloads.*;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.repo.OrganisationInvitationRepo;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.repo.OrganisationMemberRepo;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.service.OrganisationMemberService;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.entity.OrgMemberRoleEntity;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.repo.MemberRoleRepo;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.service.MemberRoleService;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.service.PermissionCheckerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
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
    private final MemberRoleService memberRoleService;
    private final PermissionCheckerService permissionChecker;
    private final MemberRoleRepo memberRoleRepository;

    @Value("${frontend.base-url}")
    private String frontendBaseUrl;

    @Transactional
    @Override
    public boolean inviteMember(UUID organisationId, String email, UUID roleId) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember currentMember = organisationMemberRepo
                .findByAccountAndOrganisation(currentUser, organisation)
                .orElseThrow(() -> new AccessDeniedException("You are not a member of this organisation"));

        permissionChecker.checkMemberPermission(currentMember, "ORGANISATION", "manageMembers");

        if (organisationMemberRepo.existsByAccountEmailAndOrganisation(email, organisation)) {
            return false;
        }

        OrgMemberRoleEntity roleToAssign = memberRoleRepository.findByOrganisationAndRoleId(organisation, roleId).orElseThrow(
                ()-> new ItemNotFoundException("Role not found in organisation")
        );


        String currentUserRoleName = currentMember.getMemberRole().getRoleName();

        if ("OWNER".equals(roleToAssign.getRoleName()) && !"OWNER".equals(currentUserRoleName)) {
            throw new AccessDeniedException("Only organisation owner can invite other owners");
        }


        OrganisationInvitation invitation = new OrganisationInvitation();
        invitation.setOrganisation(organisation);
        invitation.setInviter(currentUser);
        invitation.setEmail(email);
        invitation.setMemberRole(roleToAssign);
        invitation.setToken(generateUniqueToken());
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setCreatedAt(LocalDateTime.now());
        invitation.setExpiresAt(LocalDateTime.now().plusDays(7));

        OrganisationInvitation savedInvitation = organisationInvitationRepo.save(invitation);

        return sendInvitationEmail(savedInvitation);
    }
    @Override
    public void addOwnerAsMember(OrganisationEntity organisation, AccountEntity owner) {
        Optional<OrganisationMember> existingMember = organisationMemberRepo
                .findByAccountAndOrganisation(owner, organisation);

        if (existingMember.isEmpty()) {
            OrganisationMember ownerMember = new OrganisationMember();
            ownerMember.setOrganisation(organisation);
            ownerMember.setAccount(owner);
            ownerMember.setStatus(MemberStatus.ACTIVE);
            ownerMember.setJoinedAt(LocalDateTime.now());
            ownerMember.setInvitedBy(owner.getId());

            ownerMember.setMemberRole(memberRoleService.assignRoleToMember(ownerMember, "OWNER"));

            organisationMemberRepo.save(ownerMember);
        }
    }


    @Transactional
    @Override
    public AcceptInvitationResponse acceptInvitation(String token) throws ItemNotFoundException, InvitationAlreadyProcessedException, InvitationExpiredException, RandomExceptions, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationInvitation invitation = organisationInvitationRepo.findByToken(token)
                .orElseThrow(() -> new ItemNotFoundException("Invalid invitation token"));

        validateInvitation(currentUser, invitation);

        Optional<AccountEntity> existingUser = accountRepo.findByEmail(invitation.getEmail());

        if (existingUser.isEmpty()) {
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
        member.setMemberRole(memberRoleService.assignRoleToMember(member, invitation.getMemberRole().getRoleName()));
        member.setStatus(MemberStatus.ACTIVE);
        member.setJoinedAt(LocalDateTime.now());
        member.setInvitedBy(invitation.getInviter().getId());

        organisationMemberRepo.save(member);

        // Step 6: Update invitation status
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setRespondedAt(LocalDateTime.now());
        organisationInvitationRepo.save(invitation);

        AcceptInvitationResponse response = new AcceptInvitationResponse();
        response.setOrganisationId(member.getOrganisation().getOrganisationId());
        response.setOrganisationName(member.getOrganisation().getOrganisationName());

        return response;
    }


    @Transactional
    @Override
    public boolean declineInvitation(String token) throws ItemNotFoundException, InvitationAlreadyProcessedException, InvitationExpiredException, RandomExceptions, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        // Step 1: Find an invitation by token
        OrganisationInvitation invitation = organisationInvitationRepo.findByToken(token)
                .orElseThrow(() -> new ItemNotFoundException("Invalid invitation token"));

        // Step 2: Validate invitation (throws exceptions if invalid)
        validateInvitation(currentUser, invitation);

        // Step 3: Mark as declined
        invitation.setStatus(InvitationStatus.DECLINED);
        invitation.setRespondedAt(LocalDateTime.now());
        organisationInvitationRepo.save(invitation);

        return true;
    }

    @Override
    public void revokeInvitation(UUID organisationId, UUID invitationId) throws ItemNotFoundException, InvitationAlreadyProcessedException, InvitationExpiredException, RandomExceptions, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(
                ()-> new ItemNotFoundException("Organisation not found")
        );

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        OrganisationInvitation invitation = organisationInvitationRepo.findByOrganisationAndInvitationId(organisation, invitationId).orElseThrow(
                () -> new ItemNotFoundException("Invitation not exist in this organisation")
        );


        permissionChecker.checkMemberPermission(member, "ORGANISATION", "manageMembers");


        //We can revoke only pending invitations
        if(invitation.getStatus() != InvitationStatus.PENDING){
            throw new AccessDeniedException("You can only revoke pending invitations");
        }
        organisationInvitationRepo.delete(invitation);

    }

    @Override
    public InvitationInfoResponse getInvitationInfo(String token) throws ItemNotFoundException, AccessDeniedException, InvitationAlreadyProcessedException, InvitationExpiredException, RandomExceptions {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationInvitation invitation = organisationInvitationRepo.findByToken(token)
                .orElseThrow(() -> new ItemNotFoundException("Invalid invitation token"));

        // Step 2: Validate invitation (throws exceptions if invalid)
        if (!currentUser.getEmail().equals(invitation.getEmail())) {
            throw new AccessDeniedException("This invitation does not belong to you");
        }

        // Step 3: Check if the invitation is expired (but don't update the status here)
        boolean isExpired = invitation.getExpiresAt().isBefore(LocalDateTime.now());

        // Step 4: Build a safe response
        InvitationInfoResponse response = new InvitationInfoResponse();
        response.setOrganisationName(invitation.getOrganisation().getOrganisationName());
        response.setOrganisationDescription(invitation.getOrganisation().getOrganisationDescription());
        response.setInviterName(invitation.getInviter().getUserName());
        response.setRole(invitation.getMemberRole().getRoleName());
        response.setToken(invitation.getToken());
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

    @Override
    public OrganisationMembersOverviewResponse getAllMembersAndInvitations(UUID organisationId) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember currentMember = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(currentMember, "ORGANISATION", "manageMembers");

        List<OrganisationMember> allMembers = organisationMemberRepo.findAllByOrganisation(organisation);
        List<OrganisationMemberResponse> memberResponses = allMembers.stream()
                .map(this::mapToMemberResponse)
                .toList();

        List<OrganisationInvitation> pendingInvitations = organisationInvitationRepo
                .findAllByOrganisationAndStatus(organisation, InvitationStatus.PENDING);
        List<PendingInvitationResponse> pendingResponses = pendingInvitations.stream()
                .map(this::mapToPendingInvitationResponse)
                .toList();

        List<OrganisationInvitation> declinedInvitations = organisationInvitationRepo
                .findAllByOrganisationAndStatus(organisation, InvitationStatus.DECLINED);
        List<PendingInvitationResponse> declinedResponses = declinedInvitations.stream()
                .map(this::mapToPendingInvitationResponse)
                .toList();

        OrganisationMembersOverviewResponse response = new OrganisationMembersOverviewResponse();
        response.setOrganisationName(organisation.getOrganisationName());
        response.setTotalMembers(allMembers.size());
        response.setTotalPendingInvitations(pendingInvitations.size());
        response.setTotalActiveMembers((int) organisationMemberRepo.countByOrganisationAndStatus(organisation, MemberStatus.ACTIVE));
        response.setTotalSuspendedMembers((int) organisationMemberRepo.countByOrganisationAndStatus(organisation, MemberStatus.SUSPENDED));
        response.setMembers(memberResponses);
        response.setPendingInvitations(pendingResponses);
        response.setDeclinedInvitations(declinedResponses);

        return response;
    }

    @Override
    public List<OrganisationMemberResponse> getActiveMembers(UUID organisationId) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember currentMember = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(currentMember, "ORGANISATION", "manageMembers");

        List<OrganisationMember> activeMembers = organisationMemberRepo
                .findAllByOrganisationAndStatus(organisation, MemberStatus.ACTIVE);

        return activeMembers.stream()
                .map(this::mapToMemberResponse)
                .toList();
    }

    @Override
    public List<PendingInvitationResponse> getPendingInvitations(UUID organisationId) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember currentMember = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(currentMember, "ORGANISATION", "manageMembers");

        List<OrganisationInvitation> pendingInvitations = organisationInvitationRepo
                .findAllByOrganisationAndStatus(organisation, InvitationStatus.PENDING);

        return pendingInvitations.stream()
                .map(this::mapToPendingInvitationResponse)
                .toList();
    }

    @Override
    public UserOrganisationsOverviewResponse getMyOrganisations() throws ItemNotFoundException {

        AccountEntity currentUser = getAuthenticatedAccount();

        List<OrganisationMember> userMemberships = organisationMemberRepo.findAllByAccount(currentUser);

        // Step 3: Map to response objects
        List<UserOrganisationResponse> organisationResponses = userMemberships.stream()
                .map(this::mapToUserOrganisationResponse)
                .toList();

        // Step 4: Calculate statistics
        int totalOrganisations = organisationResponses.size();
        int ownedOrganisations = (int) organisationResponses.stream()
                .filter(UserOrganisationResponse::isOwner)
                .count();
        int memberOrganisations = totalOrganisations - ownedOrganisations;

        // Step 5: Build overview response
        UserOrganisationsOverviewResponse response = new UserOrganisationsOverviewResponse();
        response.setUserName(currentUser.getUserName());
        response.setTotalOrganisations(totalOrganisations);
        response.setOwnedOrganisations(ownedOrganisations);
        response.setMemberOrganisations(memberOrganisations);
        response.setOrganisations(organisationResponses);

        return response;
    }


    @Transactional
    @Override
    public boolean removeMember(UUID organisationId, UUID memberId) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember currentUserMember = validateOrganisationMemberAccess(currentUser, organisation);

        OrganisationMember memberToRemove = organisationMemberRepo.findByMemberIdAndOrganisation(memberId, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Member not found in this organisation"));

        // Check if it's self-removal (leaving organisation)
        boolean isSelfRemoval = memberToRemove.getAccount().getId().equals(currentUser.getId());

        if (isSelfRemoval) {
            // Special rule: OWNER cannot leave
            if ("OWNER".equals(memberToRemove.getMemberRole().getRoleName())) {
                throw new AccessDeniedException("Organisation owner cannot leave. Transfer ownership first or delete the organisation");
            }

        } else {
            // Removing another member - check permission
            permissionChecker.checkMemberPermission(currentUserMember, "ORGANISATION", "removeMembers");

            // Additional business rule: OWNER cannot be removed by anyone
            if ("OWNER".equals(memberToRemove.getMemberRole().getRoleName())) {
                throw new AccessDeniedException("Organisation owner cannot be removed");
            }

            // Additional business rule: Only OWNER can remove ADMIN
            if ("ADMIN".equals(memberToRemove.getMemberRole().getRoleName()) && !"OWNER".equals(currentUserMember.getMemberRole().getRoleName())) {
                throw new AccessDeniedException("Only organisation owner can remove administrators");
            }
        }

        organisationMemberRepo.delete(memberToRemove);
        return true;
    }

    private void validateInvitation(AccountEntity accountEntity, OrganisationInvitation invitation) throws InvitationExpiredException, InvitationAlreadyProcessedException, RandomExceptions, AccessDeniedException {

        //Before check if the invitation link is clicked with the right user?
        if (!invitation.getEmail().equals(accountEntity.getEmail())) {
            throw new AccessDeniedException("This invitation does not belong to you");
        }

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
            String invitationLink = frontendBaseUrl + "/invitation?token=" + invitation.getToken();

            return globeMailService.sendOrganisationInvitationEmail(
                    invitation.getEmail(),
                    invitation.getOrganisation().getOrganisationName(),
                    invitation.getInviter().getUserName(),
                    invitation.getMemberRole().getRoleName(),
                    invitationLink
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


    private OrganisationMemberResponse mapToMemberResponse(OrganisationMember member) {
        OrganisationMemberResponse response = new OrganisationMemberResponse();
        response.setMemberId(member.getMemberId());
        response.setUserName(member.getAccount().getUserName());
        response.setEmail(member.getAccount().getEmail());
        response.setRole(member.getMemberRole().getRoleName());
        response.setStatus(member.getStatus().toString());
        response.setJoinedAt(member.getJoinedAt());

        // Get inviter username
        if (member.getInvitedBy() != null) {
            accountRepo.findById(member.getInvitedBy())
                    .ifPresent(inviter -> response.setInvitedByUserName(inviter.getUserName()));
        }


        String roleName = member.getMemberRole().getRoleName();
        response.setOwner("OWNER".equals(roleName));
        response.setAdmin("ADMIN".equals(roleName));

        return response;
    }

    private PendingInvitationResponse mapToPendingInvitationResponse(OrganisationInvitation invitation) {
        PendingInvitationResponse response = new PendingInvitationResponse();
        response.setInvitationId(invitation.getInvitationId());
        response.setEmail(invitation.getEmail());
        response.setRole(invitation.getMemberRole().getRoleName());
        response.setStatus(invitation.getStatus().toString());
        response.setInvitedAt(invitation.getCreatedAt());
        response.setExpiresAt(invitation.getExpiresAt());
        response.setInvitedByUserName(invitation.getInviter().getUserName());

        boolean isExpired = invitation.getExpiresAt().isBefore(LocalDateTime.now());
        response.setExpired(isExpired);
        response.setCanResend(invitation.getStatus() == InvitationStatus.PENDING || invitation.getStatus() == InvitationStatus.EXPIRED);
        response.setCanRevoke(invitation.getStatus() == InvitationStatus.PENDING && !isExpired);

        return response;
    }

    private UserOrganisationResponse mapToUserOrganisationResponse(OrganisationMember membership) {
        UserOrganisationResponse response = new UserOrganisationResponse();
        OrganisationEntity org = membership.getOrganisation();

        response.setOrganisationId(org.getOrganisationId());
        response.setOrganisationName(org.getOrganisationName());
        response.setOrganisationDescription(org.getOrganisationDescription());
        response.setMyRole(membership.getMemberRole().getRoleName());
        response.setMyStatus(membership.getStatus().toString());
        response.setJoinedAt(membership.getJoinedAt());
        response.setOwnerUserName(org.getOwner().getUserName());

        String roleName = membership.getMemberRole().getRoleName();
        boolean isOwner = "OWNER".equals(roleName);
        boolean isAdmin = "ADMIN".equals(roleName);

        response.setOwner(isOwner);
        response.setAdmin(isAdmin);
        response.setCanManageMembers(isOwner || isAdmin);
        response.setCanInviteMembers(isOwner || isAdmin);

        response.setTotalMembers((int) organisationMemberRepo.countByOrganisationAndStatus(org, MemberStatus.ACTIVE));
        response.setTotalPendingInvitations((int) organisationInvitationRepo.countByOrganisationAndStatus(org, InvitationStatus.PENDING));

        return response;
    }

    private OrganisationMember validateOrganisationMemberAccess(AccountEntity account, OrganisationEntity organisation) throws ItemNotFoundException {
        OrganisationMember member = organisationMemberRepo.findByAccountAndOrganisation(account, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Member is not belong to this organisation"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }

        return member;
    }




}
