package com.example.moodic.data;

import android.util.Patterns;
import java.util.regex.Pattern;

/**
 * ValidationUtils — centralized input validation for the Moodic app.
 *
 * All validation logic lives here so Activities stay thin and consistent
 * error messages are reused across screens (Login, SignUp, MoodInput).
 *
 * Password policy (RC standard):
 *   • Minimum 6 characters
 *   • At least one uppercase letter  (A-Z)
 *   • At least one digit             (0-9)
 *
 * Example valid passwords : "Music1", "Happy7Day", "Ab12cd"
 * Example invalid passwords: "music1" (no uppercase), "MUSIC" (no digit), "Ab1" (too short)
 */
public final class ValidationUtils {

    // -----------------------------------------------------------------------
    // Regex patterns
    // -----------------------------------------------------------------------

    /**
     * Full password pattern (RC standard):
     *   (?=.*[A-Z])   — lookahead: at least one uppercase letter
     *   (?=.*\d)      — lookahead: at least one digit
     *   .{6,}         — total length ≥ 6 characters (any chars allowed)
     */
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Z])(?=.*\\d).{6,}$");

    /**
     * Email pattern — reuses Android's built-in Patterns class.
     * Covers the vast majority of real-world email formats.
     */
    private static final Pattern EMAIL_PATTERN = Patterns.EMAIL_ADDRESS;

    // Prevent instantiation — this is a pure utility class
    private ValidationUtils() {}

    // -----------------------------------------------------------------------
    // Email validation
    // -----------------------------------------------------------------------

    /**
     * Returns true if {@code email} is a syntactically valid email address.
     * Does NOT check whether the address actually exists.
     *
     * @param email raw string from EditText (may be null)
     * @return true ↔ non-null, non-blank, matches email pattern
     */
    public static boolean isValidEmail(String email) {
        return email != null
                && !email.trim().isEmpty()
                && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    // -----------------------------------------------------------------------
    // Password validation  (RC standard — upgraded from length-only check)
    // -----------------------------------------------------------------------

    /**
     * Returns true if {@code password} satisfies the RC password policy:
     * at least 6 characters, one uppercase letter, and one digit.
     *
     * @param password raw string from EditText (may be null)
     * @return true ↔ password meets all three criteria
     */
    public static boolean isValidPassword(String password) {
        return password != null
                && PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * Returns a human-readable explanation of why the password is invalid,
     * or {@code null} if the password is valid.
     *
     * Useful for showing targeted error messages in the UI:
     * <pre>
     *   String error = ValidationUtils.passwordError(pw);
     *   if (error != null) etPassword.setError(error);
     * </pre>
     *
     * @param password raw password string (may be null)
     * @return localised error string, or null when password is valid
     */
    public static String passwordError(String password) {
        if (password == null || password.isEmpty()) {
            return "Password cannot be empty";
        }
        if (password.length() < 6) {
            return "Password must be at least 6 characters";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "Password must contain at least one uppercase letter";
        }
        if (!password.matches(".*\\d.*")) {
            return "Password must contain at least one digit";
        }
        // All rules passed
        return null;
    }

    // -----------------------------------------------------------------------
    // Mood / free-text validation
    // -----------------------------------------------------------------------

    /**
     * Returns true if the mood string is non-null and non-blank.
     *
     * @param mood text from the mood EditText
     * @return true ↔ usable mood input
     */
    public static boolean isValidMood(String mood) {
        return mood != null && !mood.trim().isEmpty();
    }

    // -----------------------------------------------------------------------
    // Convenience: combined login-form check
    // -----------------------------------------------------------------------

    /**
     * Validates both email and password for the Login/SignUp forms.
     * Returns the first error found, or {@code null} if both are valid.
     *
     * @param email    email field value
     * @param password password field value
     * @return error message string, or null when both inputs are valid
     */
    public static String validateLoginForm(String email, String password) {
        if (!isValidEmail(email)) {
            return "Please enter a valid email address";
        }
        String pwError = passwordError(password);
        if (pwError != null) {
            return pwError;
        }
        return null; // all good
    }
}