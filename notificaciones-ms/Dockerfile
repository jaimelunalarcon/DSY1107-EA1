# Jar precompilado: ./mvnw verify && docker build ...
FROM public.ecr.aws/amazoncorretto/amazoncorretto:21-al2023-headless

WORKDIR /app
COPY target/notificaciones-ms.jar app.jar
USER 65534:65534
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
