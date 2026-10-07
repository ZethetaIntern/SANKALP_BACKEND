package com.sankalp.backend.service;

import com.sankalp.backend.entity.Login;
import com.sankalp.backend.repository.LoginRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProfileServiceImpl implements ProfileService {

    private final LoginRepository loginRepository;

    public ProfileServiceImpl(LoginRepository loginRepository) {
        this.loginRepository = loginRepository;
    }

    @Override
    public Login getAdminProfile() {
        // 1. Search for any user with role ADMIN
        List<Login> admins = loginRepository.findAll().stream()
                .filter(u -> "ADMIN".equalsIgnoreCase(u.getRole()))
                .toList();

        if (!admins.isEmpty()) {
            return admins.get(0);
        }

        // 2. Fallback to username "admin" or ID 1
        return loginRepository.findByUsername("admin")
                .or(() -> loginRepository.findById(1L))
                .orElseThrow(() -> new RuntimeException("Admin not found"));
    }

    @Override
    public Login updateAdminProfile(Login profile) {
        Login admin = null;

        if (profile.getId() != null) {
            admin = loginRepository.findById(profile.getId()).orElse(null);
        }

        if (admin == null && profile.getEmployeeId() != null && !profile.getEmployeeId().isBlank()) {
            admin = loginRepository.findByEmployeeId(profile.getEmployeeId()).orElse(null);
        }

        if (admin == null && profile.getUsername() != null && !profile.getUsername().isBlank()) {
            admin = loginRepository.findByUsername(profile.getUsername()).orElse(null);
        }

        if (admin == null) {
            admin = getAdminProfile();
        }

        if (profile.getUsername() != null && !profile.getUsername().isBlank()) {
            admin.setUsername(profile.getUsername());
        }
        if (profile.getEmail() != null && !profile.getEmail().isBlank()) {
            admin.setEmail(profile.getEmail());
        }
        if (profile.getMobileNumber() != null) {
            admin.setMobileNumber(profile.getMobileNumber());
        }
        if (profile.getEmployeeId() != null && !profile.getEmployeeId().isBlank()) {
            admin.setEmployeeId(profile.getEmployeeId());
        }
        if (profile.getPassword() != null && !profile.getPassword().isBlank()) {
            admin.setPassword(profile.getPassword());
        }

        return loginRepository.save(admin);
    }

    @Override
    public List<Login> getAllAdmins() {
        return loginRepository.findAll().stream()
                .filter(u -> "ADMIN".equalsIgnoreCase(u.getRole()))
                .toList();
    }

    @Override
    public Login addAdmin(Login login) {
        if (login.getRole() == null || login.getRole().isBlank()) {
            login.setRole("ADMIN");
        }
        if (login.getAccountStatus() == null || login.getAccountStatus().isBlank()) {
            login.setAccountStatus("ACTIVE");
        }
        return loginRepository.save(login);
    }
}