# How to Run
First, make sure your environment is set up correctly (see the **Environment** section). Then, in the project root, run:

```bash
gradle style
```

# Configuration
The configuration file located at **config/test.properties** allows you to enable / disable violations in the check. For any violation category, you can set it to false to disable it, or true to enable. For example, setting:

```properties
ClassNames=false
```

Means the tool will no longer report class name violations.


# About
SmartStyle is an extension of Gradestyle [1] used in my research paper to identify code-style violations in open source Java repositories. 

There are 2 categories of violations: 
1. Code-style Violations
2. Java Best Programming Practice Violations

Below is a summary of the violations this tool is able to detect. Note that some of these are disabled by default. Violations that are new in this tool (not in Gradestyle) are marked with a star.

# Code Style Violations

1. **Class Names**  
   Must follow UpperCamelCase and use nouns or noun phrases.

2. **Method Names**  
   Must follow lowerCamelCase and use verb phrases.

3. **Variable Names**  
   - Regular variables: lowerCamelCase, no underscores or dollar signs.  
   - Constants (static final): ALL_UPPERCASE with underscores.

4. **Package Names**  
   Must be all lowercase and match the directory structure.

5. **Class Javadocs** ✰  
   Public classes and enums must have a Javadoc with at least 10 words.

6. **Constructor Javadocs** ✰  
   Public constructors must have a Javadoc with at least 10 words.

7. **Method Javadocs** ✰  
   Public methods must have a Javadoc with at least 10 words and include appropriate tags (`@param`, `@return`, `@throws`).

8. **Field Javadocs** ✰  
   Fields must have a Javadoc comment.

9. **Javadoc Formatting**  
   Javadocs must be correctly formatted and include all required tags.

## Java Best Programming Practices Violations

10. **Missing @Override** ✰  
    Overridden methods must include the `@Override` annotation.

11. **Empty Catch Blocks** ✰  
    Catch blocks must not be empty unless justified by a comment (or part of test code with expected exceptions).

12. **Unqualified Static Access** ✰  
    Static methods or fields must be accessed via the class name, not via an instance or method call.

13. **Finalize Override** ✰  
    Must not override `Object.finalize()`.

14. **Private Instances**  
    Instance fields should be private or protected with access through getters/setters.

15. **String Concatenation in Loops**  
    Avoid string concatenation in loops; use `StringBuilder` instead.

16. **Useless Code**  
    Unused imports, variables, methods, or commented-out code should be removed.


# Environment

To build and run this project, you'll need the following:

- Java 17 or higher 
- Gradle 8.0.1 
- Internet connection

This project is cross-platform and works on Windows, macOS, and Linux.

# References
[1] C. Iddon, N. Giacaman and V. Terragni, "GRADESTYLE: GitHub-Integrated and Automated Assessment of Java Code Style," 2023 IEEE/ACM 45th International Conference on Software Engineering: Software Engineering Education and Training (ICSE-SEET), Melbourne, Australia, 2023, pp. 192-197, doi: 10.1109/ICSE-SEET58685.2023.00024. keywords: {Training;Java;Computer languages;Codes;Source coding;Static analysis;Turning;computing education;code style;programming courses;automated marking;GitHub;Java programming language},



