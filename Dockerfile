# Using OpenJDK 21 as the base image
FROM openjdk:21-jdk-slim

# Set the working directory
WORKDIR /app

# Copy the Gradle wrapper files
COPY gradle/ gradle/
COPY gradlew gradlew.bat ./
COPY build.gradle settings.gradle ./

# Give execution permissions to the gradle wrapper
RUN chmod +x ./gradlew

# Copy the source code
COPY src/ src/

# Build the application
RUN ./gradlew build -x test

# Create logs directory
RUN mkdir -p /app/logs

# Copy the built JAR (with fallback)
RUN if [ -f build/libs/*.jar ]; then \
        cp build/libs/*.jar /app/app.jar; \
    else \
        echo "No JAR found, will use class files"; \
    fi

# Expose the port
EXPOSE 8080

# Create a startup script that tries JAR first, then class files
RUN echo '#!/bin/sh\nif [ -f app.jar ]; then\n  echo "Running JAR file..."\n  java -jar app.jar\nelse\n  echo "Running from class files..."\n  java -cp build/classes/java/main Main\nfi' > start.sh && \
    chmod +x start.sh

# Command to run the application
CMD ["./start.sh"]