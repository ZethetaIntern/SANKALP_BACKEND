package com.sankalp.backend.service;

import com.sankalp.backend.entity.Login;
import java.util.List;

public interface ProfileService {

    Login getAdminProfile();

    Login updateAdminProfile(Login login);

    List<Login> getAllAdmins();

    Login addAdmin(Login login);
}