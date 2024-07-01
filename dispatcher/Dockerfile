FROM maven:3.8.4-openjdk-17 as builder
ARG jar_name=dispatcher-0.0.1-SNAPSHOT.jar

COPY target/$jar_name usr/local/$jar_name
WORKDIR /usr/local

#RUN ["-ls", "."]
#RUN ["pwd"]
CMD ["java", "-jar", "dispatcher-0.0.1-SNAPSHOT.jar"]
