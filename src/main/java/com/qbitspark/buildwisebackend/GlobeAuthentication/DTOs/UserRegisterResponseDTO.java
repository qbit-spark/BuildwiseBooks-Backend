package com.qbitspark.buildwisebackend.GlobeAuthentication.DTOs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.Date;
import java.util.UUID;

@Data
public class UserRegisterResponseDTO {
    private UUID userId;
    private String phoneNumber;
    private String userName;
    @JsonIgnore
    private String password;
    private String email;
    private Date createdAt;
    private Date editedAt;
    private String roles;
    private Boolean isVerified;
}
