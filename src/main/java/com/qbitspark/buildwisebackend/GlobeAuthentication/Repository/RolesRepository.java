package com.qbitspark.buildwisebackend.GlobeAuthentication.Repository;


import com.qbitspark.buildwisebackend.GlobeAuthentication.entity.Roles;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
public interface RolesRepository extends JpaRepository<Roles, UUID> {
    Optional<Roles> findByRoleName(String name);
}
