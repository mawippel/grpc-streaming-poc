# Boilerplate
FROM openjdk:17-jdk-slim

ARG LIBS_ARG
ARG RESOURCES_ARG

ENV LIBS=$LIBS_ARG
ENV RESOURCES=$RESOURCES_ARG

RUN mkdir app
ADD ${LIBS}grpc-streaming-poc.jar /app/
ADD ${RESOURCES}application.yaml /app/

WORKDIR /app
ENTRYPOINT exec java $JAVA_OPTS -Dspring.config.location=/app/application.yaml -jar grpc-stream-service.jar $APP_OPTS
