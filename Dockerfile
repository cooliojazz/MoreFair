FROM azul/zulu-openjdk-alpine:17-jre
#FROM openjdk:17-jdk-alpine // Currently missing some java random extension?
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8080
ENTRYPOINT ["java","-Xmx750M","-jar","/app.jar"]