package controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.ui.Model;

import entities.Course;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import services.CourseService;

@Controller
public class CourseController {

    @Autowired
    private CourseService courseService;

    @GetMapping("/add-course")
    public String addCoursePage(HttpSession session, Model model) {
        if (session.getAttribute("admin") == null) {
            return "redirect:/admin/login";
        }
        model.addAttribute("course", new Course());
        return "add-course";
    }

    @PostMapping("/add-course")
    public String saveCourse(
            @Valid Course course,
            BindingResult bindingResult,
            HttpSession session) {

        if (session.getAttribute("admin") == null) {
            return "redirect:/admin/login";
        }

        if (bindingResult.hasErrors()) {
            return "add-course";
        }

        courseService.saveCourse(course);

        return "redirect:/admin/courses";
    }
}
