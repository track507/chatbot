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
- JDK 22 (configurable to JDK 11 or higher)
- Maven
- SQLite3
- Qdrant
- Ollama (with two models):
  - `nomic-embed-text` (for embeddings)
  - `gemma3:4b` (for chat generation)
  - These are the default models. Gemma3:4b is included as a ModelFile

### Optional (but required if you choose to generate visuals)
- SchemaSpy and dependencies (already included: `./tools/schemaspy-6.2.4.jar`):
  - Graphviz (for relationship diagrams)
  - Java 8+ or OpenJDK 1.8+
  - SQLite JDBC driver (already included: `./tools/sqlite-jdbc-3.49.1.0.jar`)


## Setting Up

### 1. Install Docker or Docker Desktop
   - Download Links:
      - [MacOS](https://docs.docker.com/desktop/setup/install/mac-install/)
      - [Windows](https://docs.docker.com/desktop/setup/install/windows-install/)
      - [Linux](https://docs.docker.com/engine/install/)
   - If on windows, I recommend using WSL. See [here](https://learn.microsoft.com/en-us/windows/wsl/install) for installation instructions.


### 2. Get a Qdrant Container (Docker Desktop Terminal, WSL, Linux, MacOS):
   - IMPORTANT: MAKE SURE 6333 IS AN OPEN PORT.
      ```
      sudo docker run -d \
      --name qdrant \
      -p 6333:6333 \
      --restart=no \
      qdrant/qdrant
      ```

### 3. Get Ollama
   - Download Links:
      - [MacOS](https://ollama.com/download/mac)
      - [Windows](https://ollama.com/download/windows)
      - [Linux](https://ollama.com/download/linux)
   - Run Ollama with an embedding model and LLM
      ```
      ollama pull nomic-embed-text
      ollama create Advisor -f ./ModelFiles/Gemma3-4b
      ollama run Advisor
      ```

### 4. Manual Configuration (Optional)
   These files are required. `options.json` is used to story and pull information to connect to the LLM, Ollama, Qdrant, Database, etc. It's also meant to keep persistence if the program crashes or if the database is ever updated. 

   `db.properties` is used to story the properties for SchemaSpy. It cannot be ignored, but a generic one will be created for you if you do not wish to use SchemaSpy.
   
   * #### `options.json`
      - `options.json` is generated at runtime, but you can manually set it as well in the root directory. The default configuration is:
         ```json
         {
            "embedModel" : "nomic-embed-text",
            "LLMModel" : "Advisor",
            "ollamaEndpoint" : "http://localhost:11434",
            "qdrantEndpoint" : "http://localhost:6333",
            "qdrantCollection" : "chatbot",
            "ollamaEmbedEndpoint" : "http://localhost:11434/api/embeddings",
            "database" : "main.db"
         }
         ```
      - Each field must be present. If the `options.json` is missing/broken or any of the fields are missing, the program will notify and ask to generate one during setup.

   * #### `db.properties`
      - `db.properties` is also generated at run time, or you can manually set one up in the root directory. If one is not present, it will create one at setup. The default configuration is:
         ```txt
         schemaspy.t=sqlite-xerial
         schemaspy.dp=tools/sqlite-jdbc-3.49.1.0.jar
         schemaspy.db=main.db
         schemaspy.s=main
         schemaspy.u=ignored
         schemaspy.o=visuals/schemaspy
         schemaspy.cat=%
         schemaspy.url=jdbc:sqlite:main.db
         schemaspy.imageformat=svg
         schemaspy.renderer=:
         schemaspy.fontsize=12
         schemaspy.font=Arial
         schemaspy.degree=1
         schemaspy.maxdet=200
         ```

### 5. Interactive Setup
   You can now use `mvn compile exec:java` to run the chatbot. The current interactive process goes as follows:
      
   1. Previous Setup
      - If a `options.json` is found, and has all the required fields, prompt to use it, run defaults, or manually configure it during the setup.
   2. Rerun database setup.
      - If yes, files named `run.sql` and `export.sql` are required.
         - `run.sql` would run any number of `.sql` files to ensure the database is updated.
         - `export.sql` is used to export the sql into a `database_export.txt` file. This file is what the program embeds to pass into the vector store (Qdrant) in chunks so that we can use semantic search
   3. Revectorize Embeddings
      - This is recommended if the above step is used, but not required. This will make a DELETE request to the vector store collection so that "old" embeds are removed.
   4. Vectorzing User Info (REQUIRED)
      - Due to the nature of this program and logging inW, each user is stored into the vector store. For example:
         ```txt
         User Profile:
         - id: 1001
            - firstname: Bob
            - lastname: Miller
         - majors: 
            - MATH - Mathematics
         - concentrations: 
         - current_classes: 
            - MATH101 - Calculus I
            - BIO201 - Genetics
            - MATH201 - Linear Algebra
         - total_credit_hours: 11
         - department: 
            - name: Mathematics
            - id: MATH
         - remaining_hours: 121
         ```
   5. SchemaSpy Generation (Optional)
      - NOTE: Due to Powershell parsing issues, and the `dot` command not being properly called, there is a `ssFixer` function in `Utils.java` that fixes *some* of the errors when using SchemaSpy. I have not fully tested this in Linux/Unix terminals, only PowerShell and Cmd prompt.
      - Must have GraphViz installed.
      - The SchemaSpy jar is in the `./tools` directory.
   6. Chat Away!
