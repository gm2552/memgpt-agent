# MemGPT Sample

This project is a proof of concept that implements a simplified and abridge implementation of the MemGPT research 
[paper](https://research.memgpt.ai/).  It implements the concept as an agent and showcases how the it can
be integrated either as a simple REST service or as an [MCP](https://www.anthropic.com/news/model-context-protocol) server.

## Overview

This project implements the MemGPT concept using Spring Boot and Spring AI and exposes its functionality as
a REST service and an MCP Server.  In addition, it contains a simple ChatBot command line application that can
integrate the agent with either of these mechanisms.  The project is comprised of the following sub-projects:

- core-agent: Defines and implements the MemGPT interfaces.  This includes a configuration API.
- restful-agent-app: A wrapper service around the agent to expose it via simple REST interface.
- memgpt-mcp-server: A wrapper service around the agent to expose it as an MCP server.
- simple-chat-bot-app: A command line based chat application that communicates with the agent via the REST or MCP interfaces.

## Prerequisites

- Java 17 or later
- Gradle
- OpenAI API key

## Core Agent

The core-agent is component that comprises the interfaces and implementation of the MemGPT agent.  It is responsible for maintaining
configuration and state of agents which includes:

- System prompt
- Tools
- Core memory
- Archival memory
- Message persistence including messages outside the context window

It is also responsible for the core agent functions including but not limited to:

- Message compilation
- LLM communication
- Tool calling
- Memory management include context window self editing
- Message retrieval and archiving
- Message summarization  

Externally, the agent exposes its functionality through three main interfaces:

- **AgentManager**: Configures and maintains agent instances.
- **AgentLoader**: Instantiates agent instances.
- **Agent**: Defines the core agent chat functionality.


## Running the Applications

The simple-chat-bot-app communicates with either the restful-agent-app or the mgmgpt-mcp-server.  Choose which server application you would
like the ChatBot app to communicate with, configure that app, and run it.  You will then configure the ChatBot app to use the server application
of your choice and run it.

### Server Application

Both the MCP and REST server applications simply wrap the core agent, so their configurations are identical.  Execute the following steps to
run either server application of your choice.


1. Set the OpenAPI Key environment variable

```bash
export OPENAI_API_KEY=your-api-key
```


2. Build the application 

```bash
./gradlew :restful-agent-app:build
```

or

```bash
./gradlew :memgpt-mcp-server:build
```

3. Run the Server Application


```bash
java -jar ./restful-agent-app/build/libs/restful-agent-app-0.0.1-SNAPSHOT.jar
```

or

```bash
java -jar ./memgpt-mcp-server/build/libs/memgpt-mcp-server-0.0.1-SNAPSHOT.jar
```

The server application will load and is ready for incoming transactions from the ChatBot application.

#### Archival Memory

MemGPT archival memory requires the use of a vector store and Embeddings model.  By default, archival memory
is disabled, but you can activate it by enabling the `pgvector` Spring profile.  If you are using OpenAI as
your embeddings provider and postgres as your database, no additional configuration should be required.  Other providers
may require additional configuration which is beyond the scope of this README.

### ChatBot Application

The ChatBot application can communicate with either the REST application or the MCP server application; by default
it will attempt to communicate with the REST Application.  When executing against an MCP server, the ChatBot can either
communicate directly with the MCP server manage the context window and create chat completions, 
or it an use a custom Spring AI Advisor to only manage the context window memory and handle creating chat completions
itself.  

Execute the following steps to configure and run the ChatBot application:


1. Set the OpenAPI Key environment variable

```bash
export OPENAI_API_KEY=your-api-key
```

2. Configure Target Server Application (Optional if targeting the REST Application).

Edit the `simple-chat-bot-app\src\main\resource\application.yaml` file and update the `spring.profiles.active` setting with the following value
depending on the targeted server application.

- **rest (default):** Generates chat completions over a REST API with the MemGPT server.
- **mcp:** Generates chat completions over an MCP API with the MemGPT server.
- **restadvisor:** Generates chat completions by communicating directly with the LLM and using the MemGPT server to only manage memory over a RESTful API.
- **mdcadvisor:** Generates chat completions by communicating directly with the LLM and using the MemGPT server to only manage memory over an MCP API.


3. Build the application 

```bash
./gradlew :simple-chat-bot-app:build
```

4. Run the ChatBot Application

```bash
java -jar ./simple-chat-bot-app/build/libs/simple-chat-bot-app-0.0.1-SNAPSHOT.jar
```

The application will first ask for a User ID which can be anything; it is mainly used as a unique identifier for the chat session.

