package com.qbitspark.buildwisebackend.GlobeAuthentication.Repository;

import com.qbitspark.buildwisebackend.GlobeAuthentication.entity.AccountEntity;
import com.qbitspark.buildwisebackend.GlobeAuthentication.entity.UserOTP;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserOTPRepository extends JpaRepository<UserOTP, Long> {
    UserOTP findUserOTPByUser(AccountEntity userManger);
}
