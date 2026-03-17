FROM maven:3.9-eclipse-temurin-17

WORKDIR /app
COPY . .

RUN mvn package

# 👇 FORCE RUN COMMAND (fix)
ENTRYPOINT ["java","-jar","target/smartsticker-server-1.0.jar"]