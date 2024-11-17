## Welcome to Marvin, the smart house robot

Marvin is a robot that can connect to an OpenHAB server, read item states and send commentds via 
the REST API.

It's brain is implemented in Spring AI.

## How to run Marvin

To use Marvin, you need to have a running OpenHAB server.

The main module that brings Marvin to life is marvin.interaction.web. It is a Spring Boot application
that contains the application.yml configuration file. You need to set some parameters there.

The Spring AI implemented brain is in the marvin.brain.springai module. It needs a vector store to work.
Use a docker container to run the vector store. There is a docker compose file in the marvin.brain.springai docker folder.

## How to build Marvin

Maven is used to build Marvin. You can build the whole project with the following command:

```
