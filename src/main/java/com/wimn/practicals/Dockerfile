FROM maven:3.9.9-eclipse-temurin-21

WORKDIR /app

COPY . .

RUN mvn -B -DskipTests package

EXPOSE 10000

CMD ["mvn", "-B", "-Dexec.mainClass=com.wimn.StandaloneWebLauncher", "exec:java"]
