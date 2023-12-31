# Use the maven 3 image as the base
FROM maven:3

# Copy the built jar file into the image
COPY target/${JAR_FILE} /app/co-developer-gpt-engine.jar

# Set the working directory to /mnt
WORKDIR /mnt

# Define the volume
VOLUME ["/mnt"]

# Command to run when the container starts
CMD ["java", "-jar", "/app/co-developer-gpt-engine.jar"]
