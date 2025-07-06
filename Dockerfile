FROM gradle:8.5-jdk21 AS build
WORKDIR /usr/app/
COPY . .
RUN gradle clean build

FROM openjdk:21-slim
WORKDIR /usr/app/
COPY --from=build /usr/app/build/libs/fileservice.jar app.jar
EXPOSE 8080
# Environment variables for Kubernetes deployment
ENV FOS_ROOT_DIR=/fosroot
ENV REDIS_HOST=fos-redis-master.default.svc.cluster.local
ENV REDIS_PORT=6379
ENV REDIS_PASSWORD=<retrieved_password>

ENTRYPOINT ["java", "-jar", "app.jar"]
