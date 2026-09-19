package services;



import dto.MCQQuestion;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import entities.Attendance;
import entities.Certificate;
import entities.Course;
import entities.Purchase;
import entities.TestResult;
import entities.User;
import repositories.AttendanceRepository;
import repositories.CertificateRepository;
import repositories.CourseRepository;
import repositories.PurchaseRepository;
import repositories.TestResultRepository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class GeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);

    @Value("${gemini.api.key}")
    private String apiKey;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private TestResultRepository testResultRepository;

    // =========================
    // AI CHATBOT
    // =========================

    public String askAI(String question, User user) {

        try {

            String url =
                    "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key="
                            + apiKey;

            String studentContext =
                    buildStudentContext(user);

            String context =
                    """
                    You are Learnify AI Assistant.

                    You have access to the student's Learnify account.

                    Student Data:
                    %s

                    Instructions:
                    - Answer using the student data whenever relevant.
                    - If the question is about attendance, use attendance data.
                    - If the question is about purchased courses, use purchased courses.
                    - If the question is about certificates, use certificate data.
                    - If the question is about test results, use test data.
                    - Be friendly and professional.

                    Student Question:
                    %s
                    """
                    .formatted(studentContext, question);

            String requestBody =
                    """
                    {
                      "contents": [
                        {
                          "parts": [
                            {
                              "text": "%s"
                            }
                          ]
                        }
                      ]
                    }
                    """
                    .formatted(
                            context
                                    .replace("\\", "\\\\")
                                    .replace("\"", "\\\"")
                                    .replace("\n", "\\n"));

            RestTemplate restTemplate =
                    new RestTemplate();

            String response =
                    restTemplate.postForObject(
                            url,
                            requestBody,
                            String.class);

            JSONObject json =
                    new JSONObject(response);

            return json
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");

        } catch (Exception e) {

            logger.error("Gemini request failed", e);

            return "Sorry, I couldn't process your request right now.";
        }
    }

    // =========================
    // BUILD STUDENT CONTEXT
    // =========================

    private String buildStudentContext(User user) {

        StringBuilder sb =
                new StringBuilder();

        sb.append("Student Name: ")
                .append(user.getName())
                .append("\n");

        sb.append("Email: ")
                .append(user.getEmail())
                .append("\n\n");

        // =========================
        // ATTENDANCE
        // =========================

        List<Attendance> attendanceList =
                attendanceRepository.findByUserId(
                        user.getId());

        long present =
                attendanceList.stream()
                        .filter(a ->
                                "Present".equalsIgnoreCase(
                                        a.getStatus()))
                        .count();

        long absent =
                attendanceList.size() - present;

        double percentage = 0;

        if (!attendanceList.isEmpty()) {

            percentage =
                    ((double) present /
                            attendanceList.size()) * 100;
        }

        sb.append("Attendance:\n");
        sb.append("Present Classes: ")
                .append(present)
                .append("\n");

        sb.append("Absent Classes: ")
                .append(absent)
                .append("\n");

        sb.append("Attendance Percentage: ")
                .append(String.format("%.2f", percentage))
                .append("%\n\n");

        // =========================
        // PURCHASED COURSES
        // =========================

        sb.append("Purchased Courses:\n");

        List<Purchase> purchases =
                purchaseRepository.findByUserId(
                        user.getId());

        if (purchases.isEmpty()) {

            sb.append("No purchased courses\n");
        }

        List<Long> purchasedCourseIds = purchases.stream()
                .map(Purchase::getCourseId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        java.util.Map<Long, Course> coursesById =
                new java.util.HashMap<>();

        if (!purchasedCourseIds.isEmpty()) {
            for (Course course : courseRepository.findAllById(purchasedCourseIds)) {
                coursesById.put(course.getId(), course);
            }
        }

        for (Purchase purchase : purchases) {

            Course course = coursesById.get(purchase.getCourseId());

            if (course != null) {

                sb.append("- ")
                        .append(course.getCourseName())
                        .append("\n");

                sb.append("  Duration: ")
                        .append(course.getDuration())
                        .append("\n");

                sb.append("  Price: ₹")
                        .append(course.getPrice())
                        .append("\n");
            }
        }

        sb.append("\n");

        // =========================
        // CERTIFICATES
        // =========================

        sb.append("Certificates:\n");

        List<Certificate> certificates =
                certificateRepository.findByUserId(
                        user.getId());

        if (certificates.isEmpty()) {

            sb.append("No certificates earned\n");
        }

        for (Certificate certificate : certificates) {

            sb.append("- ")
                    .append(certificate.getCourseName())
                    .append(" (Issued: ")
                    .append(certificate.getIssueDate())
                    .append(")\n");
        }

        sb.append("\n");

        // =========================
        // TEST RESULTS
        // =========================

        sb.append("Test Results:\n");

        List<TestResult> results =
                testResultRepository.findByUserId(
                        user.getId());

        if (results.isEmpty()) {

            sb.append("No test results available\n");
        }

        for (TestResult result : results) {

            sb.append("- ")
                    .append(result.getCourseName())
                    .append(" : ")
                    .append(result.getScore())
                    .append("/")
                    .append(result.getTotalQuestions())
                    .append(" (")
                    .append(result.getTestDate())
                    .append(")\n");
        }

        return sb.toString();
    }

    // =========================
    // CERTIFICATE TEXT
    // =========================

    public String generateCertificateText(
            String studentName,
            String courseName,
            int score) {

        if (score >= 90) {

            return studentName
                    + " demonstrated outstanding dedication, technical excellence, and problem-solving skills while completing the "
                    + courseName
                    + " program.";
        }

        if (score >= 75) {

            return studentName
                    + " successfully completed the "
                    + courseName
                    + " program with strong performance and commitment to learning.";
        }

        return studentName
                + " completed the "
                + courseName
                + " program and demonstrated consistent effort throughout the learning journey.";
    }
    
    public String generateTestQuestions(
            String courseName){

        String prompt =
        """
        Generate 10 MCQ questions for %s.

        Format:

        Q1:
        A)
        B)
        C)
        D)

        Answer:
        """.formatted(courseName);

        return askAI(prompt, null);
    }
    
    public List<MCQQuestion> generateMCQTest(
            String courseName) {

        try {

            String prompt =
            """
            Generate exactly 10 multiple choice questions
            for %s.

            Return ONLY valid JSON.

            Example:

            [
              {
                "question":"What is JVM?",
                "optionA":"Java Virtual Machine",
                "optionB":"Java Variable Machine",
                "optionC":"Java Version Manager",
                "optionD":"None",
                "correctAnswer":"A"
              }
            ]
            """
            .formatted(courseName);

            String url =
            "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key="
            + apiKey;

            ObjectMapper mapper = new ObjectMapper();

            String requestBody = mapper.writeValueAsString(
                    java.util.Map.of(
                            "contents",
                            java.util.List.of(
                                    java.util.Map.of(
                                            "parts",
                                            java.util.List.of(
                                                    java.util.Map.of("text", prompt))))));

            RestTemplate restTemplate =
                    new RestTemplate();

            String response =
                    restTemplate.postForObject(
                            url,
                            requestBody,
                            String.class);

            org.json.JSONObject json =
                    new org.json.JSONObject(response);

            String aiText =
                    json.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");

            aiText =
                    aiText.replace("```json", "")
                          .replace("```", "")
                          .trim();

            return mapper.readValue(
                    aiText,
                    new TypeReference<List<MCQQuestion>>() {
                    });

        } catch(Exception e) {

            logger.error("Gemini request failed", e);

            return new ArrayList<>();
        }
    }
}