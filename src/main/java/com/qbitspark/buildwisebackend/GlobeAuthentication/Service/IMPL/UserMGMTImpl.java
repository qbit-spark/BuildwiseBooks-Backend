package com.qbitspark.buildwisebackend.GlobeAuthentication.Service.IMPL;

import com.fasterxml.jackson.core.JsonProcessingException;

import com.qbitspark.buildwisebackend.GlobeAdvice.Exceptions.*;
import com.qbitspark.buildwisebackend.GlobeAuthentication.DTOs.UserLoginDTO;
import com.qbitspark.buildwisebackend.GlobeAuthentication.DTOs.UserRegisterDTO;
import com.qbitspark.buildwisebackend.GlobeAuthentication.DTOs.UserRegisterResponseDTO;
import com.qbitspark.buildwisebackend.GlobeAuthentication.Entity.GlobeUserEntity;
import com.qbitspark.buildwisebackend.GlobeAuthentication.Entity.Roles;
import com.qbitspark.buildwisebackend.GlobeAuthentication.Payloads.LoginResponse;
import com.qbitspark.buildwisebackend.GlobeAuthentication.Payloads.RefreshTokenResponse;
import com.qbitspark.buildwisebackend.GlobeAuthentication.Repository.GlobeUserRepository;
import com.qbitspark.buildwisebackend.GlobeAuthentication.Repository.RolesRepository;
import com.qbitspark.buildwisebackend.GlobeAuthentication.Service.EmailOTPService;
import com.qbitspark.buildwisebackend.GlobeAuthentication.Service.UserManagementService;
import com.qbitspark.buildwisebackend.GlobeResponseBody.GlobalJsonResponseBody;
import com.qbitspark.buildwisebackend.GlobeSecurity.JWTProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class UserMGMTImpl implements UserManagementService {

    private final GlobeUserRepository globeUserRepository;
    private final RolesRepository rolesRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JWTProvider tokenProvider;
    private final EmailOTPService emailOTPService;
    private final ModelMapper modelMapper;


    @Override
    public GlobeUserEntity registerUser(UserRegisterDTO userManagementDTO) throws ItemReadyExistException, RandomExceptions, ItemNotFoundException {

        //Todo: check the existence of user
        if (globeUserRepository.existsByPhoneNumberOrEmailOrUserName(userManagementDTO.getPhoneNumber(),
                userManagementDTO.getEmail(),
                userManagementDTO.getUserName())) {
            throw new ItemReadyExistException("User with provided credentials already exist, login");
        }

        GlobeUserEntity userManger = convertDTOToEntity(userManagementDTO);
        userManger.setUserName(generateUserName(userManagementDTO.getEmail()));
        userManger.setCreatedAt(new Date());
        userManger.setEditedAt(new Date());
        userManger.setIsVerified(false);
        userManger.setPassword(passwordEncoder.encode(userManagementDTO.getPassword()));
        //Todo: lets set the user role
        Set<Roles> roles = new HashSet<>();
        Roles userRoles = rolesRepository.findByRoleName("ROLE_NORMAL_USER").get();
        roles.add(userRoles);
        userManger.setRoles(roles);
        GlobeUserEntity saved = globeUserRepository.save(userManger);


        //Todo: send OTP or email OTP HERE.....
       // smsotpService.generateAndSendPSWDResetOTP(userManger.getPhoneNumber());

        // Todo: Send the OTP via Email for registration
        String emailHeader = "Welcome to BuildWise Books Support!";
        String instructionText = "Please use the following OTP to complete your registration:";
        emailOTPService.generateAndSendEmailOTP(userManger, emailHeader, instructionText);

        return saved;
    }

    @Override
    public LoginResponse loginUser(UserLoginDTO userLoginDTO) throws VerificationException, ItemNotFoundException {

            String input = userLoginDTO.getPhoneEmailOrUserName();
            String password = userLoginDTO.getPassword();

            // Determine the type of input (phone number, email, or username)
            GlobeUserEntity user = null;
            if (isEmail(input)) {
                user = globeUserRepository.findByEmail(input).orElseThrow(
                        () -> new ItemNotFoundException("User with provided email does not exist")
                );
            } else if (isPhoneNumber(input)) {
                user = globeUserRepository.findUserMangerByPhoneNumber(input).orElseThrow(() -> new ItemNotFoundException("phone number do not exist"));
            } else {
                user = globeUserRepository.findByUserName(input).orElseThrow(
                        () -> new ItemNotFoundException("User with provided username does not exist")
                );
            }
            if (user != null) {

                Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                user.getUserName(),
                                password));

                if (!user.getIsVerified()) {
                    throw new VerificationException("Account not verified, please verify");
                }

                SecurityContextHolder.getContext().setAuthentication(authentication);
                String accessToken = tokenProvider.generateAccessToken(authentication);
                String refreshToken = tokenProvider.generateRefreshToken(authentication);

                LoginResponse loginResponse = new LoginResponse();
                loginResponse.setAccessToken(accessToken);
                loginResponse.setRefreshToken(refreshToken);
                loginResponse.setUserData(user);


                return loginResponse;

            } else {
                throw new ItemNotFoundException("User with provided details does not exist, register");
            }

    }

    @Override
    public RefreshTokenResponse refreshToken(String refreshToken) throws TokenInvalidException {
        try {
            // First, validate that this is specifically a refresh token
            if (!tokenProvider.validToken(refreshToken, "REFRESH")) {
                throw new TokenInvalidException("Invalid token");
            }

            // Get username from a token
            String userName = tokenProvider.getUserName(refreshToken);

            // Retrieve user from database
            GlobeUserEntity user = globeUserRepository.findByUserName(userName)
                    .orElseThrow(() -> new ItemNotFoundException("User not found"));

            // Create authentication with user authorities
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    user.getUserName(),
                    null,
                    mapRolesToAuthorities(user.getRoles())
            );

            // Generate only a new access token, not a new refresh token
            String newAccessToken = tokenProvider.generateAccessToken(authentication);

            // Build response
            RefreshTokenResponse refreshTokenResponse = new RefreshTokenResponse();
            refreshTokenResponse.setNewToken(newAccessToken);

            return refreshTokenResponse;

        } catch (TokenExpiredException e) {
            throw new TokenInvalidException("Refresh token has expired. Please login again");
        } catch (Exception e) {
            throw new TokenInvalidException("Failed to refresh token: " + e.getMessage());
        } finally {
            // Clear security context after token refresh
            SecurityContextHolder.clearContext();
        }
    }

    @Override
    public List<GlobeUserEntity> getAllUser() {
        return globeUserRepository.findAll();
    }

    @Override
    public GlobeUserEntity getSingleUser(UUID userId) throws ItemNotFoundException {
        return globeUserRepository.findById(userId).orElseThrow(() -> new ItemNotFoundException("No such user"));
    }


    private String generateUserName(String email) {

        StringBuilder username = new StringBuilder();
        for (int i = 0; i < email.length(); i++) {
            char c = email.charAt(i);
            if (c != '@') {
                username.append(c);
            } else {
                break;
            }
        }
        return username.toString();
    }


    //Todo: convert DTO to Entity
    public GlobeUserEntity convertDTOToEntity(UserRegisterDTO userManagementDTO) {
        return modelMapper.map(userManagementDTO, GlobeUserEntity.class);
    }

    private boolean isPhoneNumber(String input) {
        // Regular expression pattern for validating phone numbers
        String phoneRegex = "^\\+(?:[0-9] ?){6,14}[0-9]$";
        // Compile the pattern into a regex pattern object
        Pattern pattern = Pattern.compile(phoneRegex);
        // Use the pattern matcher to test if the input matches the pattern
        return input != null && pattern.matcher(input).matches();
    }

    private boolean isEmail(String input) {
        // Regular expression pattern for validating email addresses
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        // Compile the pattern into a regex pattern object
        Pattern pattern = Pattern.compile(emailRegex);
        // Use the pattern matcher to test if the input matches the pattern
        return input != null && pattern.matcher(input).matches();
    }

    private Collection<? extends GrantedAuthority> mapRolesToAuthorities(Set<Roles> roles) {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getRoleName()))
                .collect(Collectors.toList());
    }


}
