# Use a base image that supports sbt
FROM sbtscala/scala-sbt:openjdk-17.0.2_1.8.1_2.13.10

# Set the working directory
WORKDIR /app

# Copy only the build.sbt and project directory
COPY build.sbt .
COPY project/ ./project/

# Create dummy source files to force dependency resolution
RUN mkdir -p src/main/scala && touch src/main/scala/Dummy.scala

# Run sbt update to download and cache dependencies
RUN sbt update

# Remove the dummy source files
RUN rm -rf src

EXPOSE 8080

# Run sbt with continuous compilation and running
CMD ["sbt", "~run"]