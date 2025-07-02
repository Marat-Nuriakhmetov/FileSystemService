FROM gradle:8.5-jdk21 AS build
WORKDIR /usr/app/
COPY . .
RUN gradle clean build

FROM openjdk:21-slim
WORKDIR /usr/app/
COPY --from=build /usr/app/build/libs/fileservice.jar app.jar
EXPOSE 8080
RUN mkdir /usr/app/rootdir
ENV FILE_OPERATION_SERVICE_ROOT_DIR /usr/app/rootdir
ENTRYPOINT ["java", "-jar", "app.jar"]
