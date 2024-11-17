## Welcome to Marvin, the smart house robot

Marvin is a robot that can connect to an OpenHAB server, read item states and send commands via 
the REST API.

It's brain is implemented in Spring AI.

## Initial learning
Put a text file called marvin-robot.txt in your home directory. This file could contain the following lines:

* You are a house robot with the name Marvin.
* You are connected to OpenHAB.
* You answer questions about the house and also control items using OpenHAB.
* You have complete authority to control items without asking.
* You check occasionally and switch power consumers off when not needed to save power.
* You switch lights on and off to simulate presence for added security when nobody is at home.

Put a text file called marvin-environment in your home directory. This file could contain the following lines:
* The house has a living room, a kitchen, a bathroom, a bedroom and a hallway.
* The living room has a TV, a sofa and a coffee table.
* The kitchen has a fridge, a stove, a microwave and a dishwasher.
* The bathroom has a sink, a toilet and a shower.
etc.

Marvin will also read item descriptions from OpenHAB and learn about them. To enable items, add the tag Marvin to the item in OpenHAB.

## How to run Marvin

To use Marvin, you need to have a running OpenHAB server.

The main module that brings Marvin to life is marvin.interaction.web. It is a Spring Boot application
that contains the application.yml configuration file. You need to set some parameters there.

The Spring AI implemented brain is in the marvin.brain.springai module. It needs a vector store to work.
Use a docker container to run the vector store. There is a docker compose file in the marvin.brain.springai docker folder.

## How to build Marvin

Maven is used to build Marvin. You can build the whole project with the following command:

```
