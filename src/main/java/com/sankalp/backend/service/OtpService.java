package com.sankalp.backend.service;
import com.sankalp.backend.entity.Employee;

import com.sankalp.backend.entity.Login;
import com.sankalp.backend.entity.Otp;
import com.sankalp.backend.repository.LoginRepository;
import com.sankalp.backend.repository.OtpRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import java.util.Random;

import com.sankalp.backend.entity.Attendance;
import com.sankalp.backend.repository.AttendanceRepository;
import java.time.LocalDate;
import java.time.LocalTime;

import com.sankalp.backend.repository.EmployeeRepository;

@Service
public class OtpService {

    @Autowired
    private MailService mailService;

    @Autowired
    private LoginRepository loginRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    private final OtpRepository repository;

    public OtpService(OtpRepository repository) {
        this.repository = repository;
    }

    public String generateOtp(String employeeId) {

        String otp = String.format("%06d", new Random().nextInt(999999));

        List<Otp> existing = repository.findAllByEmployeeId(employeeId);

        Otp otpEntity;

        if (!existing.isEmpty()) {
            otpEntity = existing.get(0);

            // Delete duplicate OTP rows if they exist
            if (existing.size() > 1) {
                repository.deleteAll(existing.subList(1, existing.size()));
            }

        } else {
            otpEntity = new Otp();
            otpEntity.setEmployeeId(employeeId);
        }

        otpEntity.setOtp(otp);
        otpEntity.setExpiryTime(LocalDateTime.now().plusMinutes(5));

        repository.save(otpEntity);

        Login login = loginRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        System.out.println("Generated OTP : " + otp);

        // Send email asynchronously in background so SMTP timeouts don't block login
        new Thread(() -> {
            try {
                mailService.sendOtpEmail(login.getEmail(), otp);
            } catch (Exception e) {
                System.err.println("Could not send OTP email via SMTP: " + e.getMessage());
            }
        }).start();

        return otp;
    }

    public boolean verifyOtp(String employeeId, String enteredOtp) {

        // Master/demo fallback OTP so employees can always log in even if email is blocked/delayed
        if ("123456".equals(enteredOtp)) {
            recordAttendance(employeeId);
            return true;
        }

        List<Otp> otpList = repository.findAllByEmployeeId(employeeId);

        if (otpList.isEmpty()) {
            return false;
        }

        Otp saved = otpList.get(0);

        if (saved.getExpiryTime().isBefore(LocalDateTime.now()))
            return false;

        if (!saved.getOtp().equals(enteredOtp)) {
            return false;
        }

        recordAttendance(employeeId);

        return true;
    }

    private void recordAttendance(String employeeId) {
        boolean alreadyMarked = attendanceRepository.findByEmployeeId(employeeId)
                .stream()
                .anyMatch(a -> a.getDate().equals(LocalDate.now().toString()));

        if (!alreadyMarked) {
            Employee employee = employeeRepository.findById(employeeId).orElse(null);
            if (employee != null) {
                Attendance attendance = new Attendance();
                attendance.setEmployeeId(employee.getId());
                attendance.setEmployeeName(employee.getName());
                attendance.setDepartment(employee.getDepartment());
                attendance.setDate(LocalDate.now().toString());
                attendance.setCheckIn(LocalTime.now().withNano(0).toString());
                attendance.setCheckOut("-");
                attendance.setStatus("Present");

                attendanceRepository.save(attendance);
            }
        }
    }
}