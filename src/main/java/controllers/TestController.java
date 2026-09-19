package controllers;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import dto.MCQQuestion;
import entities.Certificate;
import entities.Course;
import entities.TestResult;
import entities.User;
import jakarta.servlet.http.HttpSession;
import repositories.PurchaseRepository;
import services.CertificateService;
import services.CourseService;
import services.GeminiService;
import services.PurchaseService;
import services.TestResultService;

@Controller
public class TestController {

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private PurchaseService purchaseService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private TestResultService testResultService;

   
    
    
    // ==========================================
    // SUBMIT AI TEST
    // ==========================================

    @PostMapping("/submit-test")
    public String submitTest(
            @RequestParam Map<String, String> answers,
            HttpSession session) {

        User user = (User) session.getAttribute("user");

        Course course =
                (Course) session.getAttribute("course");

        List<MCQQuestion> questions =
                (List<MCQQuestion>) session.getAttribute("questions");

        if (user == null || course == null || questions == null) {
            return "redirect:/my-tests";
        }

        Object startedAtValue = session.getAttribute("testStartedAt");
        if (!(startedAtValue instanceof Long)
                || System.currentTimeMillis() - (Long) startedAtValue > 15 * 60 * 1000L) {
            session.removeAttribute("testStartedAt");
            return "redirect:/my-tests?error=Test+time+expired";
        }

        if (questions.isEmpty()) {
            return "redirect:/my-tests?error=No+questions+available";
        }

        int score = 0;

        for (int i = 0; i < questions.size(); i++) {

            String userAnswer = answers.get("q" + (i + 1));

            if (userAnswer != null &&
                    userAnswer.equalsIgnoreCase(
                            questions.get(i).getCorrectAnswer())) {

                score++;
            }
        }

        double percentage =
                ((double) score / questions.size()) * 100;

        boolean passed = percentage >= 75;

        session.setAttribute("score", score);
        session.setAttribute("percentage", percentage);
        session.setAttribute("passed", passed);
        session.setAttribute("totalQuestions", questions.size());
        session.setAttribute("course", course);
        session.removeAttribute("testStartedAt");

        // ==========================
        // SAVE TEST RESULT
        // ==========================

        TestResult result = new TestResult();

        result.setUserId(user.getId());
        result.setCourseName(course.getCourseName());
        result.setScore(score);
        result.setTotalQuestions(questions.size());
        result.setTestDate(LocalDate.now());

        testResultService.saveResult(result);

        // ==========================
        // GENERATE CERTIFICATE
        // ==========================

        if (passed &&
                !certificateService.certificateExists(
                        user.getId(),
                        course.getCourseName())) {

            Certificate certificate = new Certificate();

            certificate.setUserId(user.getId());
            certificate.setStudentName(user.getName());
            certificate.setCourseName(course.getCourseName());
            certificate.setIssueDate(LocalDate.now());

            certificate.setAiRemark(
                    "Congratulations! You successfully completed the AI Assessment with an excellent performance.");

            certificateService.saveCertificate(certificate);
        }

        return "redirect:/result";
    }

    // ==========================================
    // RESULT PAGE
    // ==========================================

    @GetMapping("/result")
    public String resultPage(HttpSession session, Model model) {

        model.addAttribute(
                "score",
                session.getAttribute("score"));

        model.addAttribute(
                "percentage",
                session.getAttribute("percentage"));

        model.addAttribute(
                "passed",
                session.getAttribute("passed"));

        model.addAttribute(
                "totalQuestions",
                session.getAttribute("totalQuestions"));

        model.addAttribute(
                "course",
                session.getAttribute("course"));

        return "result";
    }

    // ==========================================
    // TEST HISTORY
    // ==========================================

    @GetMapping("/my-test-history")
    public String myTestHistory(
            HttpSession session,
            Model model) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute(
                "results",
                testResultService.getResultsByUser(user.getId()));

        return "my-test-history";
    }

}