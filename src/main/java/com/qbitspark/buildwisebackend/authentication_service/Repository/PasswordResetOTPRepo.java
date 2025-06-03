package com.qbitspark.buildwisebackend.authentication_service.Repository;

import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.authentication_service.entity.PasswordResetOTPEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PasswordResetOTPRepo extends JpaRepository<PasswordResetOTPEntity, UUID> {
PasswordResetOTPEntity findPasswordResetOTPEntitiesByAccount(AccountEntity accountEntity);
}
