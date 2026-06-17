# Project Updated

Date updated: June 16, 2026

## Executive Summary

The latest project keeps the same core JavaFX recycling rewards application, but includes cleanup and UI refinements compared with the older uploaded project.

Most important changes:

- Removed the Forgot Password feature from the login screen and controller.
- Tightened and aligned the Login and Sign Up screens.
- Fixed the logo/header position so it no longer shifts between Login and Sign Up.
- Cleaned several visible text labels and spacing issues.
- Preserved the existing visual style, colors, and JavaFX structure.
- Verified the latest project with Maven: `.\mvnw.cmd test` completed with `BUILD SUCCESS`.

## Requested Changes Implemented

### Forgot Password Removal

Changed files:

- `src/main/java/com/iskollect/controller/LoginController.java`
- `src/main/resources/com/iskollect/fxml/login.fxml`
- `README.md`

Details:

- Removed the `Forgot password?` clickable label from the login UI.
- Removed the `goToForgotPassword()` FXML handler method.
- Removed the `TextInputDialog` password reset request flow.
- Removed unused imports for `TextInputDialog` and `Optional`.
- Removed the README limitation saying Forgot Password only collects reset requests.
- Confirmed no remaining project references to:
  - `forgot`
  - `goToForgotPassword`
  - `Password reset`
  - `verification code`

### Login Screen Spacing and Alignment

Changed file:

- `src/main/resources/com/iskollect/fxml/login.fxml`

Details:

- Moved the ISKOllect logo/header upward to match the Sign Up screen.
- Matched the left recycle image height with the Sign Up screen.
- Corrected the subtitle text from `Garbage recycling Reward system` to `Garbage Recycling Rewards System`.
- Reduced the vertical gap between the password field and the Login button.
- Made the Login button wider and taller to match the Sign Up button dimensions.
- Moved the footer text upward so the screen feels less stretched.
- Expanded the error label area after removing the Forgot Password link.

### Sign Up Screen Spacing and Alignment

Changed file:

- `src/main/resources/com/iskollect/fxml/signup.fxml`

Details:

- Corrected the subtitle text to `Garbage Recycling Rewards System`.
- Adjusted the error label and Sign Up button spacing.
- Adjusted the footer text spacing.
- Corrected footer capitalization from `Already have an Account?` to `Already have an account?`.
- Kept the logo, subtitle, and tagline aligned with the Login screen.

## Other Project Differences Found

These differences were present when comparing the older upload with the latest project. Some are inherited from the newer base project, not necessarily from the latest requested edits.

### Build Configuration

Changed file:

- `pom.xml`

Details:

- Maven compiler configuration changed from explicit Java 8 `source` and `target` values to:

```xml
<release>${maven.compiler.release}</release>
```

- The latest project includes `.mvn/wrapper/maven-wrapper.jar`, making the Maven wrapper more complete.

### Module Configuration

Changed file:

- `src/main/java/module-info.java`

Details:

- Removed an unnecessary `opens com.iskollect.fxml to javafx.fxml;` entry.
- Added a final newline at the end of the file.

### Profile Picture Display

Changed files:

- `src/main/java/com/iskollect/controller/ProfileController.java`
- `src/main/resources/com/iskollect/fxml/profile.fxml`

Details:

- Added logic to display profile pictures as circular images.
- Added center-cropping behavior so uploaded profile pictures fit the circular frame better.
- Adjusted profile labels so display name, username, and badge level are centered.
- Made the profile image dimensions square.
- Cleaned up some profile screen text and prompt capitalization.

### Bottle Records Screen

Changed files:

- `src/main/java/com/iskollect/controller/BottleRecordsController.java`
- `src/main/resources/com/iskollect/fxml/bottlerecords.fxml`
- `src/main/resources/com/iskollect/style.css`

Details:

- Changed the bottle count text from `0 of bottles` / `X of bottles` to `0 bottles` / `X bottles`.
- Centered the bottle count and related badge progress labels.
- Reworked the record filter controls from individually positioned buttons into a cleaner centered filter bar.
- Renamed the Day filter button label to `Today`.
- Added reusable CSS classes:
  - `.filter-bar`
  - `.filter-button`
- Corrected several white background color values from `ffffff` to `#ffffff`.

### Rewards Screen

Changed file:

- `src/main/resources/com/iskollect/fxml/rewardsCatalog.fxml`

Details:

- Centered the current points balance area.
- Reduced the points font size slightly so large point totals fit better.
- Centered the status message under the points balance.

### Other FXML Cleanup

Changed files:

- `src/main/resources/com/iskollect/fxml/dashboard.fxml`
- `src/main/resources/com/iskollect/fxml/submitbottlepopup.fxml`
- `src/main/resources/com/iskollect/fxml/transactionhistory.fxml`

Details:

- Minor layout/style cleanup was detected in these files.
- These changes appear to be visual polish rather than feature changes.

## Packaging Differences

The older uploaded zip and latest output zip are packaged differently:

- The older zip contains an extra nested folder path:
  - `OOP_complete_updated/OOP_Complete_Project`
- The latest zip contains the project directly as:
  - `OOP_Complete_Project`
- The latest zip also includes generated Maven build output under `target/` because the project was compiled during verification.

Generated build files and IDE metadata were treated as packaging/build artifacts, not meaningful source changes.

## Verification

Command run in the latest project:

```powershell
.\mvnw.cmd test
```

Result:

- Maven build completed successfully.
- No test source files were present, so no unit tests were executed.
- Java source and FXML resources compiled/copied without build errors.

## Changed Source Files Summary

Meaningful changed files, excluding generated `target/` output and IDE metadata:

- `pom.xml`
- `README.md`
- `src/main/java/com/iskollect/controller/BottleRecordsController.java`
- `src/main/java/com/iskollect/controller/LoginController.java`
- `src/main/java/com/iskollect/controller/ProfileController.java`
- `src/main/java/module-info.java`
- `src/main/resources/com/iskollect/fxml/bottlerecords.fxml`
- `src/main/resources/com/iskollect/fxml/dashboard.fxml`
- `src/main/resources/com/iskollect/fxml/login.fxml`
- `src/main/resources/com/iskollect/fxml/profile.fxml`
- `src/main/resources/com/iskollect/fxml/rewardsCatalog.fxml`
- `src/main/resources/com/iskollect/fxml/signup.fxml`
- `src/main/resources/com/iskollect/fxml/submitbottlepopup.fxml`
- `src/main/resources/com/iskollect/fxml/transactionhistory.fxml`
- `src/main/resources/com/iskollect/style.css`

