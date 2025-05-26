package com.qbitspark.buildwisebackend.GlobeAuthentication.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.qbitspark.buildwisebackend.GlobeAdvice.Exceptions.*;
import com.qbitspark.buildwisebackend.GlobeAuthentication.DTOs.UserLoginDTO;
import com.qbitspark.buildwisebackend.GlobeAuthentication.DTOs.UserRegisterDTO;
import com.qbitspark.buildwisebackend.GlobeAuthentication.DTOs.UserRegisterResponseDTO;
import com.qbitspark.buildwisebackend.GlobeAuthentication.Entity.GlobeUserEntity;
import com.qbitspark.buildwisebackend.GlobeAuthentication.Payloads.LoginResponse;
import com.qbitspark.buildwisebackend.GlobeAuthentication.Payloads.RefreshTokenResponse;

import java.util.List;
import java.util.UUID;

public interface UserManagementService {

    GlobeUserEntity registerUser(UserRegisterDTO userManagementDTO) throws JsonProcessingException, ItemReadyExistException, RandomExceptions, ItemNotFoundException;

    LoginResponse loginUser(UserLoginDTO userLoginDTO) throws VerificationException, ItemNotFoundException;

    RefreshTokenResponse refreshToken(String refreshToken) throws TokenInvalidException;

    List<GlobeUserEntity> getAllUser();

    GlobeUserEntity getSingleUser(UUID uuid) throws ItemNotFoundException;

}
