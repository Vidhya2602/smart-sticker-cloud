FROM maven:3.9-eclipse-temurin-17

WORKDIR /app
COPY . .

# build full jar
RUN mvn package

# run jar file
CMD ["java","-jar","target/*.jar"]
