package com.sankalp.backend.controller;

import com.sankalp.backend.entity.Login;
import com.sankalp.backend.service.ProfileService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://10.205.165.151:5173"
})
public class ProfileController {

    private final ProfileService service;

    public ProfileController(ProfileService service) {
        this.service = service;
    }

    @GetMapping
    public Login getProfile() {
        return service.getAdminProfile();
    }

    @PutMapping
    public Login updateProfile(@RequestBody Login login) {
        return service.updateAdminProfile(login);
    }

    @GetMapping("/admins")
    public List<Login> getAllAdmins() {
        return service.getAllAdmins();
    }

    @PostMapping("/admins")
    public Login addAdmin(@RequestBody Login login) {
        return service.addAdmin(login);
    }
}