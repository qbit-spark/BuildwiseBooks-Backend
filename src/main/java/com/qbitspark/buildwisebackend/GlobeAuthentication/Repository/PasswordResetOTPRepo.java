package com.qbitspark.buildwisebackend.GlobeAuthentication.Repository;

import com.qbitspark.buildwisebackend.GlobeAuthentication.entity.AccountEntity;
import com.qbitspark.buildwisebackend.GlobeAuthentication.entity.PasswordResetOTPEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PasswordResetOTPRepo extends JpaRepository<PasswordResetOTPEntity, UUID> {
PasswordResetOTPEntity findPasswordResetOTPEntitiesByUser(AccountEntity accountEntity);
}
