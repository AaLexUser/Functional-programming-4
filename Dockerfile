FROM clojure:temurin-17-tools-deps-jammy AS builder

WORKDIR /build

# Copy dependency files
COPY deps.edn ./

# Copy source code
COPY src ./src

# Build uberjar
RUN clojure -X:uberjar

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy the uberjar from builder stage
COPY --from=builder /build/app.jar ./app.jar

# Run the application
CMD ["java", "-jar", "app.jar"]
