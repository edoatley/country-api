# JUnit 6 Upgrade Plan

## Current Status

- **Current JUnit Version**: 5.12.2 (via Spring Boot 3.5.7 BOM)
- **Current Gradle Version**: 8.10.2
- **Target JUnit Version**: 6.0.0

## Prerequisites

JUnit 6.0.0 requires:
1. **Java 17+** ✅ (We're using Java 21)
2. **Gradle 9.0+** ❌ (We're on 8.10.2)
3. **Build script updates** for Gradle 9 compatibility

## Why We Can't Upgrade Yet

### Issue 1: Gradle Compatibility
- Gradle 8.10.2's JUnit Platform integration is not compatible with JUnit 6
- Error: `NoClassDefFoundError: org/junit/platform/engine/OutputDirectoryCreator`
- This class was removed/moved in JUnit 6, and Gradle's test processor still references it

### Issue 2: Gradle 9 Breaking Changes
- Gradle 9.2.0 has breaking changes that require build script updates
- `sourceCompatibility` property must be set via the `java` extension instead of directly
- Other API changes may be required

### Issue 3: Spring Boot Compatibility
- Spring Boot 3.5.7 uses JUnit 5.12.2 by default
- Need to verify Spring Boot's testing infrastructure works with JUnit 6
- May need to override Spring Boot BOM JUnit version

## Upgrade Steps (When Ready)

1. **Upgrade Gradle to 9.0+**
   ```bash
   ./gradlew wrapper --gradle-version 9.2.0
   ```

2. **Update Build Scripts for Gradle 9**
   - Replace `sourceCompatibility` with `java.sourceCompatibility`
   - Replace `targetCompatibility` with `java.targetCompatibility`
   - Review other Gradle 9 breaking changes
   - **Update Shadow Plugin configuration** (see ShadowJar section below)

3. **Update JUnit Dependencies**
   - Update all JUnit dependencies to 6.0.0
   - Update `junit-platform-launcher` to 6.0.0 (unified versioning)
   - Override Spring Boot BOM JUnit version if needed

4. **Check ArchUnit Compatibility**
   - Verify `archunit-junit5:1.4.1` works with JUnit 6
   - Update if necessary

5. **Update Test Code**
   - Review JUnit 6 release notes for breaking changes
   - Update any deprecated APIs
   - Test all test suites

6. **Verify Spring Boot Integration**
   - Ensure Spring Boot test starter works with JUnit 6
   - Test Spring Boot integration tests

## ShadowJar Configuration for Gradle 9

The project uses the Shadow plugin to create fat JARs for Lambda deployment. Gradle 9 requires updates to the Shadow plugin configuration.

### Current Configuration
- **Shadow Plugin Version**: 8.1.1 (`com.github.johnrengelman.shadow`)
- **Location**: `country-service-adapters/build.gradle`
- **Purpose**: Creates `country-service-lambda-*.jar` for AWS Lambda deployment
- **Task**: `buildLambdaPackage` (custom ShadowJar task)

### Required Changes for Gradle 9

1. **Shadow Plugin Migration**
   - The Shadow plugin has been transferred to GradleUp organization
   - **New Plugin ID**: `com.gradleup.shadow` (replaces `com.github.johnrengelman.shadow`)
   - **New Version**: 9.0.1+ (compatible with Gradle 9)
   - **Migration**: Update plugin declaration in `country-service-adapters/build.gradle`

2. **Plugin Declaration Update**
   ```gradle
   // OLD (Gradle 8)
   id 'com.github.johnrengelman.shadow' version '8.1.1'
   
   // NEW (Gradle 9)
   id 'com.gradleup.shadow' version '9.0.1'
   ```

3. **Task Type Update**
   - Update `buildLambdaPackage` task type reference
   ```gradle
   // OLD
   task buildLambdaPackage(type: com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar)
   
   // NEW
   task buildLambdaPackage(type: com.gradleup.shadow.tasks.ShadowJar)
   ```

4. **Configuration Property Updates**
   - Verify `archiveBaseName`, `archiveVersion`, `archiveClassifier` still work
   - May need to use property setters: `archiveBaseName.set('country-service-lambda')`
   - Check `mergeServiceFiles()` API compatibility

5. **Dependency Configuration**
   - Verify `configurations = [project.configurations.runtimeClasspath]` still works
   - Check if `from(sourceSets.main.output)` needs updates
   - Review exclusion patterns (`exclude '**/*Test.class'`)

### Shadow Plugin Upgrade Steps

1. **Update Plugin Declaration**
   ```gradle
   plugins {
       id 'java-library'
       id 'io.spring.dependency-management' version '1.1.7'
       id 'com.gradleup.shadow' version '9.0.1' // Updated plugin ID and version
   }
   ```

2. **Update Task Type Reference**
   ```gradle
   task buildLambdaPackage(type: com.gradleup.shadow.tasks.ShadowJar) {
       // ... rest of configuration
   }
   ```

3. **Test Lambda Package Build**
   ```bash
   ./gradlew :country-service-adapters:buildLambdaPackage
   ls -lh country-service-adapters/build/libs/country-service-lambda-*.jar
   ```

4. **Verify JAR Contents**
   ```bash
   jar -tf country-service-adapters/build/libs/country-service-lambda-*.jar | head -20
   ```

5. **Test Lambda Deployment**
   - Deploy the generated JAR to AWS Lambda
   - Verify Lambda function works correctly
   - Test API Gateway integration

### Potential Issues

1. **API Changes**
   - Shadow plugin 9.x may have API changes
   - Review [Shadow plugin release notes](https://github.com/GradleUp/shadow/releases)
   - Check for deprecated methods

2. **Manifest Configuration**
   - Verify `manifest.attributes()` still works
   - Check `Main-Class` attribute is set correctly

3. **Service File Merging**
   - Verify `mergeServiceFiles()` still works as expected
   - Test for conflicts in merged service files

### References

- [Shadow Plugin (GradleUp)](https://github.com/GradleUp/shadow)
- [Shadow Plugin Documentation](https://gradleup.com/shadow/)
- [Shadow Plugin Releases](https://github.com/GradleUp/shadow/releases)

## References

- [JUnit 6.0.0 Release Notes](https://docs.junit.org/6.0.0/release-notes/)
- [Gradle 9.0 Release Notes](https://docs.gradle.org/9.0/release-notes.html)
- [Spring Boot 3.5.7 Release Notes](https://spring.io/blog/2025/10/23/spring-boot-3-5-7-available-now)

## Timeline

- **Current**: JUnit 5.12.2 (stable, working)
- **Future**: JUnit 6.0.0 (requires Gradle 9.0+ upgrade first)

