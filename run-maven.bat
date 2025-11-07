@echo off
echo Starting Spring Boot application using Maven...
cd /d "D:\EAD\gear-up-be"
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
