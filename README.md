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
- Flexible Setup: All endpoints, models, and settings stored in `options.json`
- Dual API Design: Uses gRPC for embeddings, REST API for Qdrant management
- Selenium Web scraper to pull the latest catalog information off the [ACU Catalog website](https://catalog.acu.edu/)


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
   - NOTE: If you are having trouble with loggin into Docker Hub, try `docker login --username USERNAME` or `docker login --username USERNAME docker.io`


### 2. Get a Qdrant Container (Docker Desktop Terminal, WSL, Linux, MacOS):
   - IMPORTANT: MAKE SURE 6333 AND 6334 ARE OPEN PORTS. `QdrantEmbeddingStore` requires GRPC, unlike my previous version only using REST API. In this case 6334 is the required GRPC port for `QdrantEmbeddingStore`.
      ```
      docker run -d \
         --name qdrant \
         -p 6333:6333 \
         -p 6334:6334 \
         -e QDRANT__SERVICE__GRPC_PORT=6334 \
         qdrant/qdrant
      ```
   - **IF YOU DO NOT WISH TO USE GRPC POLICY, USE MY MAIN BRANCH INSTEAD THAT SUPPORTS REST API. THIS CODE WILL NOT WORK AS LANGCHAIN4J'S DEFAULT EMBEDDING INTERFACE RELIES ON GRPC POLICY. (alternatively, you can replace `QdrantEmbeddingStore` with a supported REST Client)**

### 3. Get Ollama
   - Download Links:
      - [MacOS](https://ollama.com/download/mac)
      - [Windows](https://ollama.com/download/windows)
      - [Linux](https://ollama.com/download/linux)
   - Run Ollama with an embedding model and LLM
      ```
      ollama pull nomic-embed-text
      ollama pull gemma3:4b-it-q8_0
      ollama cp gemma3:4b-it-q8_0 gemma3-4b-it-q8_0
      ollama create Advisor -f ./ModelFiles/Advisor
      ollama run Advisor
      ```
   - Due to Windows trying to parse and create a new file, but the system doesn't allow for `:`. So we need to first pull the model and copy it using `ollama cp` to name it something with `:`. If your machine is struggling to run the model(s), you can switch it out with any model you like by changing the model name in the model file.

### 4. Manual Configuration (Optional)
   These files are required, but automatically generated upon setup. `options.json` is used to store and pull information to connect to the LLM, Ollama, Qdrant, Database, etc. It's also meant to keep persistence if the program crashes or if the database is ever updated. 

   `db.properties` is used to store the properties for SchemaSpy. It cannot be ignored, but a generic one will be created for you if you do not wish to use SchemaSpy.
   
   * #### `options.json`
      - `options.json` is generated at runtime, but you can manually set it as well in the root directory. The default configuration is:
         ```json
         {
            "embedModel": "nomic-embed-text",
            "LLMModel": "Advisor",
            "ollamaEndpoint": "http://localhost:11434",
            "ollamaEmbedEndpoint": "http://localhost:11434",
            "qdrantRestEndpoint": "http://localhost:6333",
            "qdrantGrpcHost": "localhost",
            "qdrantGrpcPort": 6334,
            "qdrantCollection": "chatbot",
            "database": "main.db"
         }
         ```
      - Each field must be present. If the `options.json` is missing/broken or any of the fields are missing, the program will notify and ask to generate one during setup.
      - In this version, `EmbeddingModel` will automatically add `/api/embed` to the end of the `ollamaEmbedEndpoint`.

   * #### `db.properties`
      - `db.properties` is also generated at run time, or you can manually set one up in the root directory. If one is not present, it will create one at setup. The default configuration is (where `DB_FILE` is the name of the database file, e.g. main.db):
         ```txt
         schemaspy.t=sqlite-xerial
         schemaspy.dp=tools/sqlite-jdbc-3.49.1.0.jar
         schemaspy.db=DB_FILE
         schemaspy.s=main
         schemaspy.u=ignored
         schemaspy.o=visuals/schemaspy
         schemaspy.cat=%
         schemaspy.url=jdbc:sqlite:DB_FILE
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
         - `run.sql` would run any number of `.sql` files to ensure the database is updated. An example of what would be in this file is:
            ```sql
            .read db_files/db.sql
            .read db_files/inserts.sql
            .read db_files/views.sql
            ```
            This would read all of the `.sql` files. This can also be used to run any SQLite or SQL command. (e.g. `select * from table` or `create table` would be valid)
         - `export.sql` is used to export SQL into `database_export.txt` (REQUIRED). The output file must match this name. This file is what the program embeds to pass into the vector store (Qdrant) in 1000 character chunks with a 200 character sliding window so that we can use semantic search. An example, and recommended, of what would be in this file is:
            ```sql
            .mode column
            .headers on
            .output database_export.txt

            .print 'Table: course'
            SELECT * FROM course;

            .print 'Table: department'
            SELECT * FROM department;

            .print 'Table: major'
            SELECT * FROM major;
            
            .output stdout
            ```
            This will export the following tables with columns and headers into a file named `database_export.txt`.
      - Example files will be provided in `./db_files`
   3. Revectorize Embeddings
      - This is recommended if the above step is used, but not required. This will make a DELETE request to the vector store collection so that "old" embeds are removed.
   4. Vectorzing User Info (REQUIRED)
      - Due to the nature of this program and logging in, each user is stored into the vector store. For example:
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
