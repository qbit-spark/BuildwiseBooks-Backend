package com.qbitspark.buildwisebackend.subcontractor_service.repo;
import com.qbitspark.buildwisebackend.subcontractor_service.entity.SubcontractorEntity;
import jakarta.validation.constraints.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface SubcontractorRepo extends JpaRepository<SubcontractorEntity, UUID> {
    boolean existsByRegistrationNumber(@NotBlank(message = "Registration number is required") @Size(min = 5, max = 50, message = "Registration number must be between 5 and 50 characters") String registrationNumber);

    boolean existsByEmail(@NotBlank(message = "Email is required") @Email(message = "Please provide a valid email address") @Size(max = 100, message = "Email must not exceed 100 characters") String email);

    boolean existsByTin(@NotBlank(message = "TIN is required") @Size(min = 9, max = 50, message = "TIN must be between 9 and 50 characters") @Pattern(regexp = "^[A-Z0-9\\-]+$", message = "TIN must contain only alphanumeric characters and hyphens") String tin);

    List<SubcontractorEntity> findByOrganisationOrganisationId(UUID organisationId);
    boolean existsByRegistrationNumberAndSubcontractorIdNot(@Size(min = 5, max = 50, message = "Registration number must be between 5 and 50 characters") String registrationNumber, UUID subcontractorId);

    boolean existsByEmailAndSubcontractorIdNot(@Email(message = "Please provide a valid email address") @Size(max = 100, message = "Email must not exceed 100 characters") String email, UUID subcontractorId);

    boolean existsByTinAndSubcontractorIdNot(@Size(min = 9, max = 50, message = "TIN must be between 9 and 50 characters") @Pattern(regexp = "^[A-Z0-9\\-]+$", message = "TIN must contain only alphanumeric characters and hyphens") String tin, UUID subcontractorId);

    List<SubcontractorEntity> findBySpecializationsIn(List<String> specializations);

    List<SubcontractorEntity> findByCompanyNameContainingIgnoreCase(String companyName);

    boolean existsByCompanyNameAndOrganisationOrganisationId(@NotBlank(message = "Company name is required") @Size(min = 2, max = 100, message = "Company name must be between 2 and 100 characters") String companyName, @NotNull(message = "Organisation ID is required") UUID organisationId);
}
