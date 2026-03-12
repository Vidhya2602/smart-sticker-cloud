FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY . .

RUN javac Server.java

EXPOSE 10000

CMD ["java", "--add-modules", "jdk.httpserver", "Server"]