package controllers;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.beans.factory.annotation.Value;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.Mac;
import java.nio.charset.StandardCharsets;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.client.RestTemplate;

import dto.MCQQuestion;
import entities.Attendance;
import entities.Certificate;
import entities.Course;
import entities.Purchase;
import entities.TestResult;
import entities.User;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import repositories.PurchaseRepository;
import services.AttendanceService;
import services.CertificateService;
import services.CourseService;
import services.GeminiService;
import services.PurchaseService;
import services.TestResultService;
import services.Userservice;

@Controller
public class UserController {

    // ==========================================
    // SERVICES
    // ==========================================

    @Autowired
    private Userservice userservice;

    @Autowired
    private CourseService courseService;

    @Autowired
    private PurchaseService purchaseService;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private TestResultService testResultService;

    @Autowired
    private GeminiService geminiService;


  
    @Autowired
    private PurchaseRepository purchaseRepository;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;
    
   
    
 

    // ==========================================
    // HOME
    // ==========================================

    @GetMapping("/index")
    public String openIndexPage() {
        return "index";
    }

    @GetMapping({"/", "/login"})
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String openRegisterPage() {
        return "register";
    }

    // ==========================================
    // AUTHENTICATION
    // ==========================================

    @PostMapping("/register")
    public String registerUser(
            @Valid User user,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            userservice.registerUserService(user);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "register";
        }

        return "redirect:/login";
    }

    @PostMapping("/login")
    public String loginUser(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            HttpServletRequest request) {

        if (userservice.loginUserService(email, password)) {

            request.changeSessionId();
            User user = userservice.getUserByEmail(email);

            session.setAttribute("user", user);

            return "redirect:/dashboard";
        }

        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {

        session.invalidate();

        return "redirect:/login";
    }

    // ==========================================
    // COURSES
    // ==========================================

    @GetMapping("/courses")
    public String coursesPage(Model model) {

        model.addAttribute(
                "courses",
                courseService.getAllCourses());

        return "courses";
    }

    @GetMapping("/course/{id}")
    public String courseDetails(
            @PathVariable Long id,
            Model model) {

        model.addAttribute(
                "course",
                courseService.getCourseById(id));

        return "course-details";
    }

    // ==========================================
    // DASHBOARD
    // ==========================================

    @GetMapping("/dashboard")
    public String dashboardPage(
            HttpSession session,
            Model model) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        // Automatically create today's attendance if missing
        attendanceService.markAbsentIfNotExists(user.getId());

        model.addAttribute(
                "courses",
                courseService.getAllCourses());

        model.addAttribute(
                "totalCourses",
                purchaseService
                        .getPurchasesByUser(user.getId())
                        .size());

        model.addAttribute(
                "attendancePercentage",
                String.format(
                        "%.1f",
                        attendanceService
                                .getAttendancePercentage(user.getId())));

        model.addAttribute(
                "certificateCount",
                certificateService
                        .getCertificateCount(user.getId()));

        model.addAttribute(
                "testCount",
                testResultService
                        .getTestCount(user.getId()));

        return "dashboard";
    }
    // ==========================================
    // PROFILE
    // ==========================================

    @GetMapping("/profile")
    public String profilePage(
            HttpSession session,
            Model model) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute(
                "courseCount",
                purchaseService.getPurchasesByUser(user.getId()).size());

        model.addAttribute(
                "certificateCount",
                certificateService.getCertificateCount(user.getId()));

        model.addAttribute(
                "testCount",
                testResultService.getTestCount(user.getId()));

        model.addAttribute(
                "attendancePercentage",
                String.format(
                        "%.1f",
                        attendanceService.getAttendancePercentage(user.getId())));

        return "profile";
    }

    // ==========================================
    // BUY COURSE
    // ==========================================

    @GetMapping("/buy-course/{id}")
    public String buyCourse(
            @PathVariable Long id,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        Course course = courseService.getCourseById(id);

        if (course == null) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Course not found.");

            return "redirect:/courses";
        }

        model.addAttribute("course", course);
        model.addAttribute("loggedInUser", user);
        model.addAttribute("razorpayKeyId", razorpayKeyId);

        return "buy-course";
    }

    // ==========================================
    // TEST PURCHASE
    // ==========================================

    @GetMapping("/test-buy/{courseId}")
    public String testBuy(
            @PathVariable Long courseId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        Course course = courseService.getCourseById(courseId);

        if (course == null) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Course not found.");

            return "redirect:/dashboard";
        }

        Purchase purchase = new Purchase();

        purchase.setUserId(user.getId());
        purchase.setCourseId(course.getId());
        purchase.setAmount(course.getPrice());
        purchase.setPaymentId("TEST_PAYMENT");

        boolean purchased =
                purchaseService.savePurchase(purchase);

        if (!purchased) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "You have already purchased this course.");

            return "redirect:/dashboard";
        }

        redirectAttributes.addFlashAttribute(
                "success",
                "Course purchased successfully.");

        return "redirect:/dashboard";
    }

    // ==========================================
    // RAZORPAY TEST
    // ==========================================

    @GetMapping("/test-razorpay")
    @ResponseBody
    public String testRazorpay() {

        return "Razorpay Config Loaded";
    }

    // ==========================================
    // RAZORPAY ORDER
    // ==========================================

    @GetMapping("/payment/order/{courseId}")
    @ResponseBody
    public ResponseEntity<?> createPaymentOrder(
            @PathVariable Long courseId,
            HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Please login to continue."));
        }

        Course course = courseService.getCourseById(courseId);

        if (course == null || course.getPrice() == null || course.getPrice() <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Course price is invalid."));
        }

        if (purchaseRepository.existsByUserIdAndCourseId(user.getId(), courseId)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "You have already purchased this course."));
        }

        try {
            long amountInPaise = Math.round(course.getPrice() * 100);

            String requestBody = """
                    {
                      "amount": %d,
                      "currency": "INR",
                      "receipt": "course_%d_user_%d_%d"
                    }
                    """.formatted(amountInPaise, courseId, user.getId(), System.currentTimeMillis());

            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(razorpayKeyId, razorpayKeySecret);
            headers.set("Content-Type", "application/json");

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.razorpay.com/v1/orders",
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    String.class);

            org.json.JSONObject order =
                    new org.json.JSONObject(response.getBody());

            return ResponseEntity.ok(Map.of(
                    "orderId", order.getString("id"),
                    "amount", order.getLong("amount"),
                    "currency", order.getString("currency")));

        } catch (Exception ex) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Unable to create payment order."));
        }
    }

    // ==========================================
    // PAYMENT SUCCESS
    // ==========================================

    @GetMapping("/payment-success")
    public String paymentSuccess(
            @RequestParam String paymentId,
            @RequestParam String orderId,
            @RequestParam String signature,
            @RequestParam Long courseId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        Course course = courseService.getCourseById(courseId);

        if (course == null) {
            redirectAttributes.addFlashAttribute("error", "Course not found.");
            return "redirect:/dashboard";
        }

        try {
            String payload = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    razorpayKeySecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"));

            String expectedSignature = bytesToHex(
                    mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));

            if (!constantTimeEquals(expectedSignature, signature)) {
                redirectAttributes.addFlashAttribute(
                        "error",
                        "Payment verification failed.");
                return "redirect:/dashboard";
            }

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(razorpayKeyId, razorpayKeySecret);

            ResponseEntity<String> paymentResponse = restTemplate.exchange(
                    "https://api.razorpay.com/v1/payments/" + paymentId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class);

            org.json.JSONObject payment =
                    new org.json.JSONObject(paymentResponse.getBody());

            if (!orderId.equals(payment.optString("order_id"))
                    || !"captured".equalsIgnoreCase(payment.optString("status"))
                    || payment.optLong("amount") != Math.round(course.getPrice() * 100)) {

                redirectAttributes.addFlashAttribute(
                        "error",
                        "Payment could not be verified.");
                return "redirect:/dashboard";
            }

            Purchase purchase = new Purchase();
            purchase.setUserId(user.getId());
            purchase.setCourseId(courseId);
            purchase.setAmount(course.getPrice());
            purchase.setPaymentId(paymentId);

            boolean saved = purchaseService.savePurchase(purchase);

            if (!saved) {
                redirectAttributes.addFlashAttribute(
                        "error",
                        "You have already purchased this course.");
                return "redirect:/dashboard";
            }

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Payment completed successfully.");

        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Payment verification failed. Please contact support if your account was charged.");
        }

        return "redirect:/dashboard";
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return java.security.MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    // ==========================================
    // ATTENDANCE
    // ==========================================

    @GetMapping("/mark-attendance")
    public String markAttendance(HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        String today =
                java.time.LocalDate.now().toString();

        Attendance existing =
                attendanceService.getAttendanceByUserAndDate(
                        user.getId(),
                        today);

        if (existing != null) {
            return "redirect:/attendance";
        }

        Attendance attendance = new Attendance();

        attendance.setUserId(user.getId());
        attendance.setDate(today);
        attendance.setStatus("Present");

        attendanceService.saveAttendance(attendance);

        return "redirect:/attendance";
    }
    // ==========================================
    // AI CHATBOT
    // ==========================================

    @PostMapping("/ask-ai")
    @ResponseBody
    public String askAI(
            @RequestParam String question,
            HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "Please login first.";
        }

        return geminiService.askAI(question, user);
    }

    // ==========================================
    // MY COURSES
    // ==========================================

    @GetMapping("/my-courses")
    public String myCourses(
            HttpSession session,
            Model model) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute(
                "myCourses",
                purchaseService.getPurchasedCourses(user.getId()));

        return "my-courses";
    }

    // ==========================================
    // MY CERTIFICATES
    // ==========================================

    @GetMapping("/my-certificates")
    public String myCertificates(
            HttpSession session,
            Model model) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute(
                "certificates",
                certificateService.getCertificatesByUser(user.getId()));

        return "my-certificates";
    }

    // ==========================================
    // VIEW CERTIFICATE
    // ==========================================

    @GetMapping("/certificate/{id}")
    public String viewCertificate(
            @PathVariable Long id,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        Certificate certificate =
                certificateService.getCertificateById(id);

        if (certificate == null
                || !user.getId().equals(certificate.getUserId())) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Certificate not found.");

            return "redirect:/my-certificates";
        }

        model.addAttribute("certificate", certificate);

        return "certificate-view";
    }

    // ==========================================
    // PAYMENT HISTORY
    // ==========================================

    @GetMapping("/payment-history")
    public String paymentHistory(
            HttpSession session,
            Model model) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute(
                "payments",
                purchaseService.getPurchasesByUser(user.getId()));

        return "payment-history";
    }
 // ==========================================
    // GENERATE AI TEST
    // ==========================================

    @GetMapping("/generate-test/{courseId}")
    public String generateTest(
            @PathVariable Long courseId,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        // Verify course purchase
        if (!purchaseRepository.existsByUserIdAndCourseId(
                user.getId(),
                courseId)) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "You have not purchased this course.");

            return "redirect:/my-tests";
        }

        // Fetch selected course
        Course course = courseService.getCourseById(courseId);

        if (course == null) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Course not found.");

            return "redirect:/my-tests";
        }

        // Generate AI questions
        List<MCQQuestion> questions =
                geminiService.generateMCQTest(
                        course.getCourseName());

        if (questions == null || questions.isEmpty()) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Unable to generate AI assessment. Please try again.");

            return "redirect:/my-tests";
        }

        // Save in session for evaluation later
        session.setAttribute("course", course);
        session.setAttribute("questions", questions);
        session.setAttribute("testStartedAt", System.currentTimeMillis());

        // Send to Thymeleaf page
        model.addAttribute("course", course);
        model.addAttribute("questions", questions);

        return "tests";
    }
   
    
 // ==========================================
    // MY AI TESTS PAGE
    // ==========================================

    @GetMapping("/my-tests")
    public String myTests(HttpSession session, Model model) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute(
                "courses",
                purchaseService.getPurchasedCourses(user.getId()));

        return "my-tests";
    }

}