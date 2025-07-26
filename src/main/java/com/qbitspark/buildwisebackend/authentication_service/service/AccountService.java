package com.qbitspark.buildwisebackend.authentication_service.service;

import com.qbitspark.buildwisebackend.globeadvice.exceptions.*;
import com.qbitspark.buildwisebackend.authentication_service.payloads.AccountLoginRequest;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.authentication_service.payloads.CreateAccountRequest;
import com.qbitspark.buildwisebackend.authentication_service.payloads.RefreshTokenResponse;

import java.util.List;
import java.util.UUID;

public interface AccountService {

    String registerAccount(CreateAccountRequest createAccountRequest) throws Exception;

    String loginAccount(AccountLoginRequest accountLoginRequest) throws Exception;

    RefreshTokenResponse refreshToken(String refreshToken) throws TokenInvalidException;

    List<AccountEntity> getAllAccounts();

    AccountEntity getAccountByID(UUID uuid) throws ItemNotFoundException;

}
