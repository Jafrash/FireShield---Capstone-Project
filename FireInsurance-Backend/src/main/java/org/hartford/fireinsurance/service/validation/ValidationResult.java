package org.hartford.fireinsurance.service.validation;

import java.util.*;

/**
 * Represents the result of business rule validation with detailed messages
 * categorized by severity level (errors, warnings, info).
 *
 * This class provides a comprehensive validation result that can accumulate
 * multiple validation issues and provide detailed feedback for business
 * rule enforcement and user guidance.
 */
public class ValidationResult {

    private final boolean valid;
    private final List<ValidationMessage> errors;
    private final List<ValidationMessage> warnings;
    private final List<ValidationMessage> infoMessages;

    /**
     * Private constructor - use Builder to create instances
     */
    private ValidationResult(Builder builder) {
        this.errors = Collections.unmodifiableList(new ArrayList<>(builder.errors));
        this.warnings = Collections.unmodifiableList(new ArrayList<>(builder.warnings));
        this.infoMessages = Collections.unmodifiableList(new ArrayList<>(builder.infoMessages));
        this.valid = this.errors.isEmpty();
    }

    /**
     * Check if validation passed (no errors)
     * @return true if validation passed, false if there are errors
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Check if validation failed (has errors)
     * @return true if validation failed
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Check if validation has warnings
     * @return true if there are warnings
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    /**
     * Check if validation has info messages
     * @return true if there are info messages
     */
    public boolean hasInfoMessages() {
        return !infoMessages.isEmpty();
    }

    /**
     * Get all error messages
     * @return List of error messages
     */
    public List<ValidationMessage> getErrors() {
        return errors;
    }

    /**
     * Get all warning messages
     * @return List of warning messages
     */
    public List<ValidationMessage> getWarnings() {
        return warnings;
    }

    /**
     * Get all info messages
     * @return List of info messages
     */
    public List<ValidationMessage> getInfoMessages() {
        return infoMessages;
    }

    /**
     * Get all messages combined
     * @return List of all validation messages
     */
    public List<ValidationMessage> getAllMessages() {
        List<ValidationMessage> allMessages = new ArrayList<>();
        allMessages.addAll(errors);
        allMessages.addAll(warnings);
        allMessages.addAll(infoMessages);
        return allMessages;
    }

    /**
     * Get error messages as formatted strings
     * @return List of error message strings
     */
    public List<String> getErrorMessages() {
        return errors.stream()
            .map(ValidationMessage::getMessage)
            .toList();
    }

    /**
     * Get warning messages as formatted strings
     * @return List of warning message strings
     */
    public List<String> getWarningMessages() {
        return warnings.stream()
            .map(ValidationMessage::getMessage)
            .toList();
    }

    /**
     * Get the first error message (if any)
     * @return First error message or null if none
     */
    public String getFirstError() {
        return errors.isEmpty() ? null : errors.get(0).getMessage();
    }

    /**
     * Get summary of validation result
     * @return Formatted summary string
     */
    public String getSummary() {
        return String.format("ValidationResult{valid=%s, errors=%d, warnings=%d, info=%d}",
                           valid, errors.size(), warnings.size(), infoMessages.size());
    }

    /**
     * Create a successful validation result
     * @return Valid ValidationResult with no messages
     */
    public static ValidationResult success() {
        return new Builder().build();
    }

    /**
     * Create a failed validation result with error
     * @param code Error code
     * @param message Error message
     * @return Invalid ValidationResult with error
     */
    public static ValidationResult error(String code, String message) {
        return new Builder().withError(code, message).build();
    }

    /**
     * Create validation result with warning
     * @param code Warning code
     * @param message Warning message
     * @return Valid ValidationResult with warning
     */
    public static ValidationResult warning(String code, String message) {
        return new Builder().withWarning(code, message).build();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ValidationResult{");
        sb.append("valid=").append(valid);

        if (!errors.isEmpty()) {
            sb.append(", errors=").append(errors.size());
        }
        if (!warnings.isEmpty()) {
            sb.append(", warnings=").append(warnings.size());
        }
        if (!infoMessages.isEmpty()) {
            sb.append(", info=").append(infoMessages.size());
        }

        sb.append("}");
        return sb.toString();
    }

    /**
     * Builder for creating ValidationResult instances
     */
    public static class Builder {
        private final List<ValidationMessage> errors = new ArrayList<>();
        private final List<ValidationMessage> warnings = new ArrayList<>();
        private final List<ValidationMessage> infoMessages = new ArrayList<>();

        /**
         * Add an error message
         * @param code Error code
         * @param message Error message
         * @return Builder for chaining
         */
        public Builder withError(String code, String message) {
            errors.add(new ValidationMessage(ValidationMessage.Level.ERROR, code, message));
            return this;
        }

        /**
         * Add an error message with context
         * @param code Error code
         * @param message Error message
         * @param context Additional context information
         * @return Builder for chaining
         */
        public Builder withError(String code, String message, Map<String, Object> context) {
            errors.add(new ValidationMessage(ValidationMessage.Level.ERROR, code, message, context));
            return this;
        }

        /**
         * Add a warning message
         * @param code Warning code
         * @param message Warning message
         * @return Builder for chaining
         */
        public Builder withWarning(String code, String message) {
            warnings.add(new ValidationMessage(ValidationMessage.Level.WARNING, code, message));
            return this;
        }

        /**
         * Add a warning message with context
         * @param code Warning code
         * @param message Warning message
         * @param context Additional context information
         * @return Builder for chaining
         */
        public Builder withWarning(String code, String message, Map<String, Object> context) {
            warnings.add(new ValidationMessage(ValidationMessage.Level.WARNING, code, message, context));
            return this;
        }

        /**
         * Add an info message
         * @param code Info code
         * @param message Info message
         * @return Builder for chaining
         */
        public Builder withInfo(String code, String message) {
            infoMessages.add(new ValidationMessage(ValidationMessage.Level.INFO, code, message));
            return this;
        }

        /**
         * Add an info message with context
         * @param code Info code
         * @param message Info message
         * @param context Additional context information
         * @return Builder for chaining
         */
        public Builder withInfo(String code, String message, Map<String, Object> context) {
            infoMessages.add(new ValidationMessage(ValidationMessage.Level.INFO, code, message, context));
            return this;
        }

        /**
         * Merge another validation result into this builder
         * @param other ValidationResult to merge
         * @return Builder for chaining
         */
        public Builder merge(ValidationResult other) {
            if (other != null) {
                errors.addAll(other.getErrors());
                warnings.addAll(other.getWarnings());
                infoMessages.addAll(other.getInfoMessages());
            }
            return this;
        }

        /**
         * Build the ValidationResult
         * @return ValidationResult instance
         */
        public ValidationResult build() {
            return new ValidationResult(this);
        }
    }

    /**
     * Represents a single validation message with level, code, and message
     */
    public static class ValidationMessage {
        private final Level level;
        private final String code;
        private final String message;
        private final Map<String, Object> context;

        /**
         * Create validation message with level, code and message
         */
        public ValidationMessage(Level level, String code, String message) {
            this(level, code, message, Collections.emptyMap());
        }

        /**
         * Create validation message with context
         */
        public ValidationMessage(Level level, String code, String message, Map<String, Object> context) {
            this.level = level;
            this.code = code;
            this.message = message;
            this.context = context != null ? Map.copyOf(context) : Collections.emptyMap();
        }

        public Level getLevel() {
            return level;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public Map<String, Object> getContext() {
            return context;
        }

        /**
         * Get formatted message with level prefix
         * @return Formatted message string
         */
        public String getFormattedMessage() {
            return String.format("[%s] %s: %s", level, code, message);
        }

        @Override
        public String toString() {
            return getFormattedMessage();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ValidationMessage)) return false;
            ValidationMessage that = (ValidationMessage) o;
            return level == that.level &&
                   Objects.equals(code, that.code) &&
                   Objects.equals(message, that.message);
        }

        @Override
        public int hashCode() {
            return Objects.hash(level, code, message);
        }

        /**
         * Validation message levels
         */
        public enum Level {
            ERROR("Error"),
            WARNING("Warning"),
            INFO("Info");

            private final String displayName;

            Level(String displayName) {
                this.displayName = displayName;
            }

            public String getDisplayName() {
                return displayName;
            }
        }
    }
}