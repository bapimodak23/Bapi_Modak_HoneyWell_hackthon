FROM openjdk:8-jre-alpine3.9
MAINTAINER modakbapi1304
COPY target/guicejettydemo-service.jar /app/server.jar
EXPOSE 8080
CMD ["-jar", "/app/server.jar"]
ENTRYPOINT ["java"]