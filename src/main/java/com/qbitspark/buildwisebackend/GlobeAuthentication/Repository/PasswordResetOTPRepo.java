package com.qbitspark.buildwisebackend.GlobeAuthentication.Repository;

import com.qbitspark.buildwisebackend.GlobeAuthentication.Entity.GlobeUserEntity;
import com.qbitspark.buildwisebackend.GlobeAuthentication.Entity.PasswordResetOTPEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PasswordResetOTPRepo extends JpaRepository<PasswordResetOTPEntity, UUID> {
PasswordResetOTPEntity findPasswordResetOTPEntitiesByUser(GlobeUserEntity globeUserEntity);
}
