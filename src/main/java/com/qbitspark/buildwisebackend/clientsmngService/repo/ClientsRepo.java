package com.qbitspark.buildwisebackend.clientsmngService.repo;

import com.qbitspark.buildwisebackend.clientsmngService.entity.ClientEntity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientsRepo extends JpaRepository<ClientEntity, UUID> {

    List<ClientEntity> findByIsActiveTrue();

    List<ClientEntity> findByNameContainingIgnoreCase(String name);

    Optional<ClientEntity> findByTin(String tin);

    Optional<ClientEntity> findByEmail(String email);

    boolean existsByTinAndClientIdNot(@Param("tin") String tin, @Param("clientId") UUID clientId);

    boolean existsByEmailAndClientIdNot(@Param("email") String email, @Param("clientId") UUID clientId);

    @Query("SELECT c FROM ClientEntity c LEFT JOIN FETCH c.projects WHERE c.clientId = :clientId")
    Optional<ClientEntity> findByClientIdWithProjects(@Param("clientId") UUID clientId);

    // Option 1: Using @Query annotation with JPQL
    @Query("SELECT c FROM ClientEntity c WHERE " +
            "(:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:email IS NULL OR LOWER(c.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
            "(:isActive IS NULL OR c.isActive = :isActive)")
    Page<ClientEntity> findBySearchCriteria(@Param("name") String name,
                                            @Param("email") String email,
                                            @Param("isActive") Boolean isActive,
                                            Pageable pageable);

    boolean existsByEmail(@Email(message = "Invalid email format") @NotBlank(message = "Email is required") String email);

    boolean existsByTin(@NotBlank(message = "The TIN is required") @Size(max = 50, message = "The tin must be less than 50 characters") String tin);
}