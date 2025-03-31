# Chatbot Advisor

This project is for my CS375 (Software Engineering II) course. This Java-based chatbot helps students plan academic paths based sql data.

## Features
- SQLite integration for querying student info
- Ollama + Qdrant vector embeddings for semantic search
- Customizable endpoints for model, embedding API, and Qdrant server
- JSON-based user context via `user_info.json` for personalized responses
- Chat interface with LLMs (Ollama-backed)

## Requirements
- Needed
  - JDK 22 (configureable 17+)
  - Maven
  - SQLite3
  - Qdrant
  - Ollama
  - Two models loaded in Ollama
    - One for embeddings (default `nomic-embed-text`)
    - One for chat generation (default `gemma3:1b`)

## Setup
- Ensure Qdrant and Ollama are running
  - Ollama models should be running as well
- Clone the repo `https://github.com/track507/chatbot.git`
- Run `mvn compile exec:java` from the root directory.