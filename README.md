# env-validator

A lightweight Java/Spring Boot library designed to validate required environment variables and application properties at startup. This library ensures that your application fails fast and provides clear, actionable feedback if the runtime environment is incorrectly configured.

---

## Why env-validator? (vs. Native Spring)

A common question is: "Doesn't Spring already fail if a variable is missing?" 

The answer is yes, but only partially. env-validator provides several architectural advantages for production systems:

| Feature | Native Spring Boot | env-validator |
| :--- | :--- | :--- |
| **Error Collection** | Fails on the first missing variable. You fix it, restart, and find another one is missing. | Scans all configurations and lists ALL missing variables in one single error report. |
| **Regex Validation** | Only checks existence. Cannot verify if a string is a valid email, URL, or UUID. | Built-in Regex support to ensure the quality of the data, not just its existence. |
| **Declarative Contract** | Secrets are often hidden in @Value expressions deep inside service classes. | Creates a clear contract at the top of your @Configuration classes, making requirements visible. |
| **Optional Defaults** | If you use a default value syntax, Spring won't fail, even if the app requires a real value. | You can mark variables as required even if they have default strings in Spring's properties. |
| **External Libraries** | Doesn't validate variables used by 3rd party libs that call System.getenv() directly. | Validates any key in the Environment, regardless of where it is used in the code. |

---

## Features
- **Annotation-based Validation**: Declaratively mark requirements using @ValidateEnv.
- **Plug-and-play Integration**: Enable the entire validation suite with a single @EnableEnvValidation annotation.
- **Regex Pattern Support**: Validate formats for emails, database URLs, and other string-based configurations.
- **Default Value Handling**: Provide fallbacks while still logging information about missing variables.
- **Support for Class and Field Levels**: Apply validation to an entire configuration class or specific fields.
- **Lightweight**: Zero heavy dependencies beyond the core Spring context and SLF4J.

---

## Project Structure
The library is organized into the following modules:
- **annotation**: Contains @ValidateEnv and @EnableEnvValidation.
- **core**: The heart of the library, including the ValidationEngine.
- **exception**: Custom exception handling for formatted error reporting.
- **spring**: Spring-specific integration including the ApplicationRunner and Configuration beans.
- **util**: Internal utilities for property resolution.

---

## Installation (Maven)

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>env-validator</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## Usage

### 1. Enable Validation
Add the @EnableEnvValidation annotation to your main Spring Boot application class or any @Configuration class.

```java
@SpringBootApplication
@EnableEnvValidation
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

### 2. Class-level Validation
Annotating a class allows you to group multiple required environment variables together. This is ideal for @Configuration classes that manage specific modules like Database or Security.

```java
@Configuration
@ValidateEnv({
    "DB_URL",
    "DB_USERNAME",
    "DB_PASSWORD"
})
public class DatabaseConfig {
}
```

### 3. Field-level Validation
You can also annotate specific fields. This is useful when you want to validate a property and also use it directly within the class.

```java
@Configuration
public class ApiConfig {
    @ValidateEnv(required = true, pattern = "^https://.*")
    private String apiEndpoint;
}
```

### 4. Regex and Default Values
The library supports complex validation rules, including regular expressions and fallback values.

```java
@Configuration
@ValidateEnv(
    value = "ADMIN_EMAIL",
    pattern = "^[A-Za-z0-9+_.-]+@(.+)$",
    defaultValue = "admin@company.com"
)
public class NotificationConfig {
}
```

---

## Technical Implementation Details
The library works by hooking into the Spring Application Life Cycle:
1. **Importing Configuration**: The @EnableEnvValidation annotation uses @Import to register the EnvValidationConfig bean.
2. **Context Scanning**: The EnvValidationRunner (which implements ApplicationRunner) is executed immediately after the ApplicationContext is refreshed.
3. **Reflection-based Inspection**: The runner identifies all beans annotated with @ValidateEnv.
4. **Validation Execution**: The ValidationEngine resolves property values via Spring's Environment, checks them against the specified rules (existence, regex, defaults), and collects any violations.
5. **Fail-Fast**: If any violations are found, a MissingEnvException is thrown, preventing the application from continuing in an invalid state.

---

## Testing
The project includes a suite of JUnit 5 tests. You can run them using:
```bash
mvn test
```
The tests verify success scenarios, missing variable failures, regex mismatches, and field-level validation across different configuration patterns.
