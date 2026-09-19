package controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import entities.User;
import jakarta.servlet.http.HttpSession;
import services.AttendanceService;

@Controller
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @GetMapping("/attendance")
    public String attendancePage(HttpSession session,
                                 Model model) {

        User user = (User) session.getAttribute("user");

        if(user == null) {
            return "redirect:/login";
        }

        model.addAttribute(
                "attendanceList",
                attendanceService.getAttendanceByUser(
                        user.getId()));

        return "attendance";
    }
}