-- Runs automatically the first time the Postgres container is created.
-- Creates one database per microservice (names are case-sensitive, hence the quotes).
CREATE DATABASE "usersurveyDB";
CREATE DATABASE "surveysDB";
CREATE DATABASE "responsesDB";
CREATE DATABASE "invitationsSurveyDB";