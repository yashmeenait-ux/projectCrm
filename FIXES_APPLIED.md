# Project CRM — Bug Fix Release Notes

## Scope
This release was created from the original `projectCrm.zip` and the supplied bug report. Changes are limited to reported defects and closely related safety fixes.

## Fixed
1. Razorpay payment completion now requires a server-created Razorpay order, HMAC signature verification, payment/order matching, captured status, and amount verification before a purchase is saved.
2. Course creation is restricted to authenticated admin sessions.
3. AI test answer submissions now send A/B/C/D identifiers, matching the Gemini `correctAnswer` representation.
4. Test passing score is consistently displayed as 75%, matching backend evaluation.
5. Gemini MCQ request JSON is serialized through Jackson instead of manual string escaping.
6. User and admin passwords now use BCrypt, with transparent one-time upgrade of existing plaintext records on successful login.
7. Admin destructive actions use POST and require an admin session.
8. Added the missing `/admin/payments` page.
9. Fixed case-sensitive Thymeleaf footer fragment references.
10. Added backend validation for registration and course creation, including duplicate-email handling.
11. Added server-side enforcement of the 15-minute AI test window.
12. Protected certificate viewing so users can only view their own certificates.
13. Added session-ID rotation after successful user/admin authentication.
14. Reduced N+1 course loading in My Courses and Gemini student context.
15. Removed accidental Markdown code fences from affected HTML templates.

## Configuration protection
`src/main/resources/application.properties` was not modified. Existing database URL, username, password, database name, and API/payment configuration remain unchanged.

## Database protection
No database was dropped, reset, migrated destructively, or directly modified by this release.

## Verification
- Maven build/test was attempted through the project Maven Wrapper.
- The wrapper could not download Maven Central dependencies in the execution environment, so no runtime test result is claimed.
- POM XML parsing passed.
- Changed Java files passed structural brace checks.
- HTML templates were parsed successfully.
- The original and fixed `application.properties` files have identical SHA-256 hashes.
