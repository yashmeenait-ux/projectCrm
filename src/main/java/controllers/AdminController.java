package controllers;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;

import entities.Certificate;
import entities.Course;
import entities.Purchase;
import entities.User;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import services.AdminActivityService;
import services.AdminService;
import services.AttendanceService;
import services.CertificateService;
import services.CourseService;
import services.GeminiService;
import services.PurchaseService;
import services.TestResultService;
import services.Userservice;

@Controller
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private Userservice userservice;

    @GetMapping("/admin/login")
    public String adminLoginPage() {
        return "admin-login";
    }
    @PostMapping("/admin/login")
    public String adminLogin(String username,
                             String password,
                             HttpSession session,
                             HttpServletRequest request) {

        if(adminService.login(username, password)) {

            request.changeSessionId();
            session.setAttribute(
                    "admin",
                    username);

            adminActivityService
                    .saveActivity(
                            "Admin Login",
                            username);

            return "redirect:/admin/dashboard";
        }

        return "redirect:/admin/login";
    }
     
    @GetMapping("/admin/dashboard")
    public String adminDashboard(
            HttpSession session,
            Model model) {

        if(session.getAttribute("admin") == null) {
            return "redirect:/admin/login";
        }

        int studentsCount =
                userservice.getAllUsers().size();

        int coursesCount =
                courseService.getAllCourses().size();

        int testsCount =
                testResultService.getAllResults().size();

        int certificatesCount =
                certificateService.getAllCertificates().size();

        int purchasesCount =
                purchaseService.getAllPurchases().size();

        double totalRevenue =
                purchaseService
                .getAllPurchases()
                .stream()
                .mapToDouble(
                        Purchase::getAmount)
                .sum();

        
        Map<String, Double> monthlyRevenue =
                new LinkedHashMap<>();

        monthlyRevenue.put("Jan", 0.0);
        monthlyRevenue.put("Feb", 0.0);
        monthlyRevenue.put("Mar", 0.0);
        monthlyRevenue.put("Apr", 0.0);
        monthlyRevenue.put("May", 0.0);
        monthlyRevenue.put("Jun", 0.0);

        purchaseService
        .getAllPurchases()
        .forEach(p -> {

            if(p.getPurchaseDate() != null){

                String month =
                p.getPurchaseDate()
                .getMonth()
                .name()
                .substring(0,3);

                monthlyRevenue.put(
                        month,

                        monthlyRevenue
                        .getOrDefault(
                                month,
                                0.0)

                        + p.getAmount());
            }
        });

        model.addAttribute(
                "monthlyRevenue",
                monthlyRevenue.values());
        // TEST RESULTS

        long passed =
                testResultService
                .getAllResults()
                .stream()
                .filter(result -> {

                    double percentage =
                            ((double) result.getScore()
                            / result.getTotalQuestions())
                            * 100;

                    return percentage >= 75;
                })
                .count();

        long failed =
                testsCount - passed;

        // ATTENDANCE

        long present =
                attendanceService
                .getAllAttendance()
                .stream()
                .filter(a ->
                        "Present"
                        .equalsIgnoreCase(
                                a.getStatus()))
                .count();

        long absent =
                attendanceService
                .getAllAttendance()
                .size()
                - present;

        model.addAttribute(
                "studentsCount",
                studentsCount);

        model.addAttribute(
                "coursesCount",
                coursesCount);

        model.addAttribute(
                "testsCount",
                testsCount);

        model.addAttribute(
                "certificatesCount",
                certificatesCount);

        model.addAttribute(
                "purchasesCount",
                purchasesCount);

        model.addAttribute(
                "totalRevenue",
                totalRevenue);

        model.addAttribute(
                "passed",
                passed);

        model.addAttribute(
                "failed",
                failed);

        model.addAttribute(
                "present",
                present);

        model.addAttribute(
                "absent",
                absent);

        model.addAttribute(
                "activities",
                adminActivityService
                .getRecentActivities());

        return "admin-dashboard";
    }
    
    
    @GetMapping("/admin/students")
    public String studentsPage(HttpSession session,
                               Model model) {

        if(session.getAttribute("admin") == null) {
            return "redirect:/admin/login";
        }

        model.addAttribute(
                "students",
                userservice.getAllUsers());

        return "admin-students";
    }
    @PostMapping("/admin/delete-student/{id}")
    public String deleteStudent(@PathVariable Long id, HttpSession session) {

        if (session.getAttribute("admin") == null) {
            return "redirect:/admin/login";
        }

        userservice.deleteUser(id);

        return "redirect:/admin/students";
    }
    @Autowired
    private CourseService courseService;
    
    @GetMapping("/admin/courses")
    public String coursesPage(HttpSession session,
                              Model model) {

        if(session.getAttribute("admin") == null) {
            return "redirect:/admin/login";
        }

        model.addAttribute(
                "courses",
                courseService.getAllCourses());

        return "admin-courses";
    }
    
    @GetMapping("/admin/payments")
    public String paymentsPage(
            HttpSession session,
            Model model) {

        if (session.getAttribute("admin") == null) {
            return "redirect:/admin/login";
        }

        model.addAttribute("payments", purchaseService.getAllPurchases());
        return "admin-payments";
    }

    @PostMapping("/admin/delete-course/{id}")
    public String deleteCourse(@PathVariable Long id, HttpSession session) {

        if (session.getAttribute("admin") == null) {
            return "redirect:/admin/login";
        }

        courseService.deleteCourse(id);

        return "redirect:/admin/courses";
    }
    
    @GetMapping("/admin/edit-course/{id}")
    public String editCoursePage(@PathVariable Long id,
                                 Model model,
                                 HttpSession session) {

        if(session.getAttribute("admin") == null) {
            return "redirect:/admin/login";
        }

        model.addAttribute(
                "course",
                courseService.getCourseById(id));

        return "edit-course";
    }

    @PostMapping("/admin/update-course")
    public String updateCourse(Course course,
                               HttpSession session) {

        if(session.getAttribute("admin") == null) {
            return "redirect:/admin/login";
        }

        courseService.saveCourse(course);

        return "redirect:/admin/courses";
    }
    
    @Autowired
    private TestResultService testResultService;
    
    @GetMapping("/admin/results")
    public String resultsPage(HttpSession session,
                              Model model) {

        if(session.getAttribute("admin") == null) {
            return "redirect:/admin/login";
        }

        model.addAttribute(
                "results",
                testResultService.getAllResults());

        return "admin-results";
    }
    
    @PostMapping("/admin/delete-result/{id}")
    public String deleteResult(@PathVariable Long id, HttpSession session) {

        if (session.getAttribute("admin") == null) {
            return "redirect:/admin/login";
        }

        testResultService.deleteResult(id);

        return "redirect:/admin/results";
    }
    @Autowired
    private AttendanceService attendanceService;
    @GetMapping("/admin/attendance")
    public String attendancePage(HttpSession session,
                                 Model model) {

        if(session.getAttribute("admin") == null) {
            return "redirect:/admin/login";
        }

        model.addAttribute(
                "attendanceList",
                attendanceService.getAllAttendance());

        return "admin-attendance";
    }
    @PostMapping("/admin/delete-attendance/{id}")
    public String deleteAttendance(@PathVariable Long id, HttpSession session) {

        if (session.getAttribute("admin") == null) {
            return "redirect:/admin/login";
        }

        attendanceService.deleteAttendance(id);

        return "redirect:/admin/attendance";
    }
    
    @Autowired
    private CertificateService certificateService;
    @GetMapping("/admin/certificates")
    public String certificatesPage(HttpSession session,
                                   Model model) {

        if(session.getAttribute("admin") == null) {
            return "redirect:/admin/login";
        }

        model.addAttribute(
                "certificates",
                certificateService.getAllCertificates());

        return "admin-certificates";
    }
    @PostMapping("/admin/delete-certificate/{id}")
    public String deleteCertificate(@PathVariable Long id, HttpSession session) {

        if (session.getAttribute("admin") == null) {
            return "redirect:/admin/login";
        }

        certificateService.deleteCertificate(id);

        return "redirect:/admin/certificates";
    }
   
  
    
   
    
    
    @GetMapping("/admin/download-certificate/{id}")
    public ResponseEntity<byte[]> downloadCertificate(
            @PathVariable Long id)
            throws Exception {

        Certificate certificate =
                certificateService.getCertificateById(id);

        Document document = new Document();

        ByteArrayOutputStream out =
                new ByteArrayOutputStream();

        PdfWriter.getInstance(document, out);

        document.open();

        document.add(
                new Paragraph(
                        "LEARNIFY ACADEMY"));

        document.add(
                new Paragraph(" "));

        document.add(
                new Paragraph(
                        "CERTIFICATE OF COMPLETION"));

        document.add(
                new Paragraph(" "));

        document.add(
                new Paragraph(
                        "Awarded To: "
                        + certificate.getStudentName()));

        document.add(
                new Paragraph(
                        "Course: "
                        + certificate.getCourseName()));

        document.add(
                new Paragraph(
                        "Issued On: "
                        + certificate.getIssueDate()));

        document.close();

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=certificate.pdf")
                .contentType(
                        MediaType.APPLICATION_PDF)
                .body(out.toByteArray());
    }
    
    @Autowired
    private GeminiService geminiService;
    
    
    @GetMapping("/admin/generate-certificate/{id}")
    public String generateCertificate(
            @PathVariable Long id) {

        User user = userservice.getUserById(id);

        if(user != null) {

            Certificate certificate =
                    new Certificate();

            certificate.setUserId(user.getId());
            certificate.setStudentName(user.getName());
            certificate.setCourseName("Java Full Stack");
            certificate.setIssueDate(
                    java.time.LocalDate.now());

            String aiRemark =
                    geminiService.generateCertificateText(
                            user.getName(),
                            "Java Full Stack",
                            95);

            certificate.setAiRemark(aiRemark);

            certificateService.saveCertificate(
                    certificate);
        }

        return "redirect:/admin/certificates";
    }
    
    
    @GetMapping("/admin/view-certificate/{id}")
    public String viewCertificate(@PathVariable Long id,
                                  Model model,
                                  HttpSession session) {

        if(session.getAttribute("admin") == null) {
            return "redirect:/admin/login";
        }

        model.addAttribute(
                "certificate",
                certificateService.getCertificateById(id));

        return "certificate-view";
    }
    @Autowired
    private PurchaseService purchaseService;
    
    @Autowired
    private AdminActivityService
            adminActivityService;
    @GetMapping("/admin/history")
    public String historyPage(
            HttpSession session,
            Model model){

        if(session.getAttribute("admin") == null){
            return "redirect:/admin/login";
        }

        model.addAttribute(
                "activities",
                adminActivityService.getRecentActivities());

        return "admin-history";
    }
    
   
    
}