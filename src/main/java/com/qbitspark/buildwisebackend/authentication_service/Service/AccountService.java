package com.qbitspark.buildwisebackend.authentication_service.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.*;
import com.qbitspark.buildwisebackend.authentication_service.payloads.AccountLoginRequest;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.authentication_service.payloads.CreateAccountRequest;
import com.qbitspark.buildwisebackend.authentication_service.payloads.LoginResponse;
import com.qbitspark.buildwisebackend.authentication_service.payloads.RefreshTokenResponse;

import java.util.List;
import java.util.UUID;

public interface AccountService {

    AccountEntity registerAccount(CreateAccountRequest createAccountRequest) throws JsonProcessingException, ItemReadyExistException, RandomExceptions, ItemNotFoundException;

    LoginResponse loginAccount(AccountLoginRequest accountLoginRequest) throws VerificationException, ItemNotFoundException;

    RefreshTokenResponse refreshToken(String refreshToken) throws TokenInvalidException;

    List<AccountEntity> getAllAccounts();

    AccountEntity getAccountByID(UUID uuid) throws ItemNotFoundException;

}
