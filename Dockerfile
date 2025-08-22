# JIVA Android build container
# Base image with JDK 17 (Debian slim) to ensure apt-get availability for SDK install
FROM openjdk:17-jdk-slim

# Prevent tzdata prompts during install
ENV DEBIAN_FRONTEND=noninteractive

# Android SDK locations
ENV ANDROID_HOME=/opt/android-sdk \
    ANDROID_SDK_ROOT=/opt/android-sdk \
    PATH=/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools:$PATH

# Install required packages
RUN apt-get update && apt-get install -y --no-install-recommends \
    wget unzip git ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Download and install Android SDK Command Line Tools
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools \
    && wget -q https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip -O /tmp/cmdline-tools.zip \
    && unzip -q /tmp/cmdline-tools.zip -d ${ANDROID_HOME}/cmdline-tools \
    && rm /tmp/cmdline-tools.zip \
    && mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest

# Accept licenses non-interactively
RUN yes | sdkmanager --licenses

# Install SDK platform, build-tools, and platform-tools matching project config (Compile SDK 35)
RUN sdkmanager \
    "platform-tools" \
    "platforms;android-35" \
    "build-tools;35.0.0"

# Set working directory
WORKDIR /app

# Copy Gradle wrapper and config first for better layer caching
COPY gradlew /app/gradlew
COPY gradle /app/gradle
COPY settings.gradle.kts build.gradle.kts gradle.properties /app/

# Make Gradle wrapper executable
RUN chmod +x ./gradlew

# Pre-download dependencies to leverage Docker layer caching (ignore failures for task listing)
RUN ./gradlew --no-daemon tasks > /dev/null 2>&1 || true

# Copy the rest of the project
COPY . /app

# Build Debug APK
RUN ./gradlew --no-daemon assembleDebug

# Default command: list APK outputs (useful when running the container)
CMD ["bash", "-lc", "ls -al app/build/outputs/apk/debug || true"]