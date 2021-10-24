FROM openjdk:8-alpine

COPY target/uberjar/wif-analytics.jar /wif-analytics/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/wif-analytics/app.jar"]
