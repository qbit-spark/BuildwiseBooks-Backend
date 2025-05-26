package com.qbitspark.buildwisebackend;

import com.qbitspark.buildwisebackend.GlobeAuthentication.Entity.Roles;
import com.qbitspark.buildwisebackend.GlobeAuthentication.Repository.RolesRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
public class BuildWiseBackendApplication implements CommandLineRunner {

    @Autowired
    private RolesRepository roleRepository;


    @Bean
    public WebClient webClient(WebClient.Builder builder) {return builder.build();}

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }


    public static void main(String[] args) {
        SpringApplication.run(BuildWiseBackendApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        createRoleIfNotExists("ROLE_SUPER_ADMIN");
        createRoleIfNotExists("ROLE_ORG_ADMIN");
        createRoleIfNotExists("ROLE_NORMAL_USER");
    }

    private void createRoleIfNotExists(String roleName) {
        Roles existingRole = roleRepository.findByRoleName(roleName).orElse(null);

        if (existingRole == null) {
            Roles newRole = new Roles();
            newRole.setRoleName(roleName);
            roleRepository.save(newRole);
        }
    }
}
