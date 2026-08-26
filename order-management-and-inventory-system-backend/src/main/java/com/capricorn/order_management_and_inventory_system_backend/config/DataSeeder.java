package com.capricorn.order_management_and_inventory_system_backend.config;

import com.capricorn.order_management_and_inventory_system_backend.entity.Role;
import com.capricorn.order_management_and_inventory_system_backend.entity.User;
import com.capricorn.order_management_and_inventory_system_backend.repository.RoleRepository;
import com.capricorn.order_management_and_inventory_system_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Create roles if they don't exist
        Role adminRole = getOrCreateRole("ADMIN");
        Role managerRole = getOrCreateRole("WAREHOUSE_MANAGER");
        getOrCreateRole("CUSTOMER"); // Ensure CUSTOMER exists too

        // Create Admin user
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@example.com")
                    .password(passwordEncoder.encode("admin123"))
                    .role(adminRole)
                    .build();
            userRepository.save(admin);
            System.out.println("Default Admin created: username=admin, password=admin123");
        }

        // Create Warehouse Manager user
        if (!userRepository.existsByUsername("manager")) {
            User manager = User.builder()
                    .username("manager")
                    .email("manager@example.com")
                    .password(passwordEncoder.encode("manager123"))
                    .role(managerRole)
                    .build();
            userRepository.save(manager);
            System.out.println("Default Manager created: username=manager, password=manager123");
        }
    }

    private Role getOrCreateRole(String name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(Role.builder().name(name).build()));
    }
}
