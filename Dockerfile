FROM clojure:temurin-17-tools-deps-jammy AS builder

WORKDIR /build

# Copy dependency files
COPY deps.edn build.clj ./

# Copy source code and data
COPY src ./src
COPY data ./data

# Build uberjar
RUN clojure -T:build uber

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy the uberjar and data from builder stage
COPY --from=builder /build/target/app.jar ./app.jar
COPY --from=builder /build/data ./data
