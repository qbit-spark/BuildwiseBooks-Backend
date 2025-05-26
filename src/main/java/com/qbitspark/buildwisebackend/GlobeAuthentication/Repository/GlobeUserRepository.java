package com.qbitspark.buildwisebackend.GlobeAuthentication.Repository;


import com.qbitspark.buildwisebackend.GlobeAuthentication.Entity.GlobeUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GlobeUserRepository extends JpaRepository<GlobeUserEntity, UUID> {
    Optional<GlobeUserEntity> findUserManagerByEmailOrPhoneNumberOrUserName(String email, String phoneNumber, String userName);
    Optional<GlobeUserEntity> findUserMangerByUserName(String userName);

    Optional<GlobeUserEntity> findByEmail(String email);
    Optional<GlobeUserEntity> findUserMangerByPhoneNumber(String phoneNumber);
    Optional<GlobeUserEntity> findByUserName(String username);
    Boolean existsByPhoneNumberOrEmailOrUserName(String phoneNumber, String email, String userName);
    Boolean existsByPhoneNumber(String phoneNumber);
    Boolean existsByEmail(String email);
    Boolean existsByUserName(String userName);

}
