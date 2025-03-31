# Chatbot Advisor

A Java-based chatbot developed for **CS375: Software Engineering II**. It assists students in planning their academic paths using SQL data, semantic search, and modern LLM tools.

## Features

- Semantic Search: Uses Qdrant + Ollama vector embeddings
- SQLite Integration: Pulls and processes real student info
- LLM Chat Interface: Chatbot backed by Ollama
- User Personalization: Loads `user_info.json` for context-aware responses
- Customizable Setup: Configure models, endpoints, and database paths via `options.json`
- Schema Visualization: Generates schema diagrams with SchemaSpy + Graphviz
- Persistence: Stores configuration in `options.json` and `db.properties` for future runs


## Requirements

### Required
- JDK 22 (configurable to JDK 17 or higher)
- Maven
- SQLite3
- Qdrant
- Ollama (with two models):
  - `nomic-embed-text` (for embeddings)
  - `gemma3:1b` (for chat generation)
  - These are the default models

### Optional (but required if you choose to generate visuals)
- SchemaSpy and dependencies:
  - Graphviz (for relationship diagrams)
  - Java 8+ or OpenJDK 1.8+
  - SQLite JDBC driver (already included: `sqlite-jdbc-3.49.1.0.jar`)

## Setup and Run

1. Start Qdrant and Ollama  
   Ensure model and embed endpoint are available (e.g., `ollama run gemma3:1b`).
2. Clone the repository:
   ```bash
   git clone https://github.com/track507/chatbot.git
   cd chatbot
   ```
3. Run:
   ```bash
   mvn compile exec:java
   ```