FROM maven:3.9-eclipse-temurin-17

WORKDIR /app
COPY . .

# compile only (not jar)
RUN mvn compile

# run compiled class directly
CMD ["java","-cp","target/classes","Server"]