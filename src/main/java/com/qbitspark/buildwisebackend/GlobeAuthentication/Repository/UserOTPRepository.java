package com.qbitspark.buildwisebackend.GlobeAuthentication.Repository;

import com.qbitspark.buildwisebackend.GlobeAuthentication.Entity.GlobeUserEntity;
import com.qbitspark.buildwisebackend.GlobeAuthentication.Entity.UserOTP;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserOTPRepository extends JpaRepository<UserOTP, Long> {
    UserOTP findUserOTPByUser(GlobeUserEntity userManger);
}
