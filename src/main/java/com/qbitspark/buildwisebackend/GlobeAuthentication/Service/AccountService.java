package com.qbitspark.buildwisebackend.GlobeAuthentication.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.qbitspark.buildwisebackend.GlobeAdvice.Exceptions.*;
import com.qbitspark.buildwisebackend.GlobeAuthentication.payloads.AccountLoginRequest;
import com.qbitspark.buildwisebackend.GlobeAuthentication.entity.AccountEntity;
import com.qbitspark.buildwisebackend.GlobeAuthentication.payloads.CreateAccountRequest;
import com.qbitspark.buildwisebackend.GlobeAuthentication.payloads.LoginResponse;
import com.qbitspark.buildwisebackend.GlobeAuthentication.payloads.RefreshTokenResponse;

import java.util.List;
import java.util.UUID;

public interface AccountService {

    AccountEntity registerAccount(CreateAccountRequest createAccountRequest) throws JsonProcessingException, ItemReadyExistException, RandomExceptions, ItemNotFoundException;

    LoginResponse loginAccount(AccountLoginRequest accountLoginRequest) throws VerificationException, ItemNotFoundException;

    RefreshTokenResponse refreshToken(String refreshToken) throws TokenInvalidException;

    List<AccountEntity> getAllAccounts();

    AccountEntity getAccountByID(UUID uuid) throws ItemNotFoundException;

}
