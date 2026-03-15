FROM maven:3.9-eclipse-temurin-17

WORKDIR /app
COPY . .

RUN mvn package

CMD ["java","-cp","target/smartsticker-server-1.0.jar","Server"]