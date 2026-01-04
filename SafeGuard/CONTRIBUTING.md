# Contributing to B-Safe

Thank you for your interest in contributing to B-Safe! This document provides guidelines for contributing to the project.

## Code of Conduct

By participating in this project, you agree to maintain a respectful and inclusive environment for everyone.

## How to Contribute

### Reporting Bugs

1. Check if the bug has already been reported in [Issues](https://github.com/yourusername/safeguard-android/issues)
2. If not, create a new issue with:
   - Clear, descriptive title
   - Steps to reproduce
   - Expected vs actual behavior
   - Device info (model, Android version)
   - Screenshots if applicable

### Suggesting Features

1. Check existing feature requests in Issues
2. Create a new issue with the "enhancement" label
3. Describe the feature and its use case
4. Explain why it would benefit users

### Pull Requests

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature-name`
3. Make your changes following our coding standards
4. Write/update tests as needed
5. Commit with clear messages: `git commit -m "Add: feature description"`
6. Push to your fork: `git push origin feature/your-feature-name`
7. Open a Pull Request

## Development Setup

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34

### Setup Steps

```bash
# Clone your fork
git clone https://github.com/YOUR_USERNAME/safeguard-android.git
cd safeguard-android

# Copy example files
cp app/google-services.json.example app/google-services.json
cp keystore.properties.example keystore.properties

# Add your API keys to local.properties
echo "MAPS_API_KEY=your_key" >> local.properties

# Build
./gradlew assembleDebug
```

## Coding Standards

### Kotlin Style

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Keep functions small and focused
- Add KDoc comments for public APIs

### Architecture

- Follow MVVM pattern
- Use Repository pattern for data access
- Keep UI logic in ViewModels
- Use Kotlin Coroutines for async operations

### Compose UI

- Use Material 3 components
- Follow accessibility guidelines
- Support both light and dark themes
- Test on multiple screen sizes

### Testing

- Write unit tests for business logic
- Write UI tests for critical flows
- Aim for meaningful test coverage

## Commit Messages

Use conventional commit format:

```
type: short description

Longer description if needed.

Fixes #123
```

Types:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `style`: Formatting
- `refactor`: Code restructuring
- `test`: Adding tests
- `chore`: Maintenance

## Review Process

1. All PRs require at least one review
2. CI must pass (build + tests)
3. Code must follow style guidelines
4. Documentation must be updated if needed

## Questions?

Feel free to open an issue for any questions about contributing.

---

Thank you for helping make B-Safe better! 🛡️
