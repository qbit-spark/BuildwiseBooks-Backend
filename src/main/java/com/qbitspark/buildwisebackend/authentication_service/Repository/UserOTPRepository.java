package com.qbitspark.buildwisebackend.authentication_service.Repository;

import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.authentication_service.entity.UserOTP;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserOTPRepository extends JpaRepository<UserOTP, Long> {
    UserOTP findUserOTPByUser(AccountEntity userManger);
}
