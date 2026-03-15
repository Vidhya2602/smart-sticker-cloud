FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY . .

# compile using libraries
RUN javac -cp "libs/*" Server.java

# run using libraries
CMD ["java","-cp",".:libs/*","--add-modules","jdk.httpserver","Server"]