package com.qbitspark.buildwisebackend.globeauthentication.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.*;
import com.qbitspark.buildwisebackend.globeauthentication.payloads.AccountLoginRequest;
import com.qbitspark.buildwisebackend.globeauthentication.entity.AccountEntity;
import com.qbitspark.buildwisebackend.globeauthentication.payloads.CreateAccountRequest;
import com.qbitspark.buildwisebackend.globeauthentication.payloads.LoginResponse;
import com.qbitspark.buildwisebackend.globeauthentication.payloads.RefreshTokenResponse;

import java.util.List;
import java.util.UUID;

public interface AccountService {

    AccountEntity registerAccount(CreateAccountRequest createAccountRequest) throws JsonProcessingException, ItemReadyExistException, RandomExceptions, ItemNotFoundException;

    LoginResponse loginAccount(AccountLoginRequest accountLoginRequest) throws VerificationException, ItemNotFoundException;

    RefreshTokenResponse refreshToken(String refreshToken) throws TokenInvalidException;

    List<AccountEntity> getAllAccounts();

    AccountEntity getAccountByID(UUID uuid) throws ItemNotFoundException;

}
