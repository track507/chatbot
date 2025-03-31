package chatbot.helpers;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.fasterxml.jackson.databind.ObjectMapper;

import chatbot.llm.Embedder;
import chatbot.llm.EmbeddingException;

public class Utils {
    private static final Scanner scanner = new Scanner(System.in);

    // login method to authenticate student
    public void login() throws SQLException {
        try {
            System.out.print("Enter your student ID: ");
            String input = scanner.nextLine().trim();

            int studentID;
            try {
                studentID = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid input: must be a numeric student ID.");
            }
    
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:abcdb.db");
                 PreparedStatement checkStmt = conn.prepareStatement("SELECT * FROM student WHERE id = ?")) {
    
                checkStmt.setInt(1, studentID);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Invalid credentials");
                    }
    
                    System.out.println("Login successful!");
                    Thread.sleep(1000);
                    generateUserInfoJson(conn, studentID);
                }
            }
        } catch (SQLException e) {
            throw e;
        } catch (InterruptedException e) {
            System.err.println("Interrupted during sleep: " + e.getMessage());
        }
    }

    private void generateUserInfoJson(Connection conn, int studentID) {
        try {
            Map<String, Object> userInfo = new LinkedHashMap<>();
            ObjectMapper mapper = new ObjectMapper();

            try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM student WHERE id = ?")) {
                stmt.setInt(1, studentID);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        userInfo.put("id", rs.getInt("id"));
                        userInfo.put("firstname", rs.getString("firstname"));
                        userInfo.put("lastname", rs.getString("lastname"));
                    }
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT sm.major, m.title FROM student_major sm JOIN major m ON sm.major = m.id WHERE sm.studentID = ?")) {
                stmt.setInt(1, studentID);
                try (ResultSet rs = stmt.executeQuery()) {
                    List<Map<String, String>> majors = new ArrayList<>();
                    while (rs.next()) {
                        majors.add(Map.of(
                            "id", rs.getString("major"),
                            "title", rs.getString("title")
                        ));
                    }
                    userInfo.put("majors", majors);
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT c.id, c.title FROM concentration c JOIN student_major sm ON c.major = sm.major WHERE sm.studentID = ?")) {
                stmt.setInt(1, studentID);
                try (ResultSet rs = stmt.executeQuery()) {
                    List<Map<String, String>> concs = new ArrayList<>();
                    while (rs.next()) {
                        concs.add(Map.of(
                            "id", rs.getString("id"),
                            "title", rs.getString("title")
                        ));
                    }
                    userInfo.put("concentrations", concs);
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT c.id, c.title FROM currentclasses cc " +
                            "JOIN course c ON cc.courseID = c.id WHERE cc.studentID = ?")) {
                stmt.setInt(1, studentID);
                try (ResultSet rs = stmt.executeQuery()) {
                    List<Map<String, String>> classes = new ArrayList<>();
                    while (rs.next()) {
                        classes.add(Map.of(
                            "id", rs.getString("id"),
                            "title", rs.getString("title")
                        ));
                    }
                    userInfo.put("current_classes", classes);
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT TotalCreditHours FROM StudentCreditHours WHERE StudentID = ?")) {
                stmt.setInt(1, studentID);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        userInfo.put("total_credit_hours", rs.getInt("TotalCreditHours"));
                    }
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT DepartmentID, DepartmentName FROM StudentDepartment WHERE StudentID = ?")) {
                stmt.setInt(1, studentID);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        userInfo.put("department", Map.of(
                            "id", rs.getString("DepartmentID"),
                            "name", rs.getString("DepartmentName")
                        ));
                    }
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT remaining_hours FROM remaining_hours2 WHERE studentID = ?")) {
                stmt.setInt(1, studentID);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        userInfo.put("remaining_hours", rs.getInt("remaining_hours"));
                    }
                }
            }

            mapper.writerWithDefaultPrettyPrinter()
                .writeValue(Paths.get("user_info.json").toFile(), userInfo);

            System.out.println("user_info.json created.");
        } catch (Exception e) {
            System.err.println("Failed to generate user_info.json: " + e.getMessage());
        }
    }

    public void setupInteractive() throws IOException, InterruptedException, SQLException, EmbeddingException {
        System.out.println("=== Chatbot Setup ===");
        Path configPath = Paths.get("options.json");
        ObjectMapper mapper = new ObjectMapper();

        SetupOptions options = null;

        if (Files.exists(configPath)) {
            try {
                options = mapper.readValue(configPath.toFile(), SetupOptions.class);
                System.out.println("Previous setup found:");
                System.out.println("  Model: " + options.model);
                System.out.println("  Ollama: " + options.ollamaEndpoint);
                System.out.println("  Ollama Embed: " + options.ollamaEmbedEndpoint);
                System.out.println("  Qdrant: " + options.qdrantEndpoint);
                System.out.println("  Database: " + options.database);

                System.out.print("Use previous setup? (yes/no): ");
                String reuse = scanner.nextLine().trim().toLowerCase();
                if (!reuse.equals("yes")) {
                    options = null;
                }
            } catch (Exception e) {
                System.err.println("Failed to read options.json, falling back to manual entry.");
                options = null;
            }
        }

        if (options == null) {
            System.out.print("Model name [default: nomic-embed-text]: ");
            String model = scanner.nextLine().trim();

            System.out.print("Ollama endpoint [default: http://localhost:11434]: ");
            String ollamaEndpoint = scanner.nextLine().trim();

            System.out.print("Ollama embedding endpoint [default: http://localhost:11434/api/embeddings]: ");
            String ollamaEmbedEndpoint = scanner.nextLine().trim();

            System.out.print("Qdrant endpoint [default: http://localhost:6333]: ");
            String qdrantEndpoint = scanner.nextLine().trim();

            System.out.print("Database name [default: main.db]: ");
            String database = scanner.nextLine().trim();

            options = new SetupOptions(
                model.isEmpty() ? null : model,
                ollamaEndpoint.isEmpty() ? null : ollamaEndpoint,
                ollamaEmbedEndpoint.isEmpty() ? null : ollamaEmbedEndpoint,
                qdrantEndpoint.isEmpty() ? null : qdrantEndpoint,
                database.isEmpty() ? null : database
            );

            // save config
            mapper.writerWithDefaultPrettyPrinter().writeValue(configPath.toFile(), options);
            System.out.println("Saved to options.json.");
        }

        Embedder embedder = new Embedder(options.ollamaEmbedEndpoint, options.model, options.qdrantEndpoint);

        System.out.print("Rerun database setup (run.sql & export.sql)? (yes/no): ");
        String dbChoice = scanner.nextLine().trim().toLowerCase();
        if (dbChoice.equals("yes")) {
            System.out.println("Running SQL scripts...");
            runSqliteScript("db_files/run.sql");
            runSqliteScript("db_files/export.sql");
        } else {
            System.out.println("Skipping database setup.");
        }

        System.out.print("Revectorize embeddings with current data? (yes/no): ");
        String vectorChoice = scanner.nextLine().trim().toLowerCase();
        if (vectorChoice.equals("yes")) {
            System.out.println("Revectorizing...");
            embedder.revectorizeAll();
        } else {
            System.out.println("Skipping vectorization.");
        }

        System.out.print("Attempting to vectorize user info...");
        Path userInfoPath = Paths.get("user_info.json");
        if (Files.exists(userInfoPath)) {
            try {
                embedder.vectorizeUserInfo(userInfoPath.toString());
            } catch (EmbeddingException e) {
                System.err.println("Failed to vectorize user_info.json: " + e.getMessage());
            }
        } else {
            System.out.println("Skipped: user_info.json not found.");
        }

        System.out.print("Would you like to generate SchemaSpy? (yes/no): ");
        String pumlChoice = scanner.nextLine().trim().toLowerCase();
        if (pumlChoice.equals("yes")) {
            generateSlitePropertiesFile();
            generateSS();
            ssFixer();
        } else {
            System.out.println("Skipping SchemaSpy generation.");
        }

        System.out.println("Setup complete.");
        Thread.sleep(2000);
    }

    // method to execute any SQLite script (.sql file)
    private void runSqliteScript(String sqlFileName) throws IOException, InterruptedException {
        String sqliteCommand = "sqlite3 abcdb.db \".read " + sqlFileName + "\"";

        ProcessBuilder processBuilder = new ProcessBuilder();

        if (System.getProperty("os.name").contains("Windows")) {
            processBuilder.command("cmd.exe", "/c", sqliteCommand);
        } else {
            processBuilder.command("sh", "-c", sqliteCommand);
        }

        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("SQLite command (" + sqlFileName + ") failed with exit code: " + exitCode);
        }

        System.out.println("SQLite script executed successfully: " + sqlFileName);
    }

    private void ssFixer() {
        try {
            Path htmlPath = Path.of("visuals/schemaspy/relationships.html");
            if(Files.exists(htmlPath)) {
                String html = Files.readString(htmlPath);
                String alertRegex = "(?s)<div class=\"alert alert-warning alert-dismissible\">.*?</div>\\s*";
                String imageBlock = """
                    <div class="relationship-diagrams">
                        <img src="diagrams/summary/relationships.implied.compact.png" alt="Implied Relationships - Compact" style="max-width: 50%; height: auto; margin: 10px 0;" />
                        <img src="diagrams/summary/relationships.implied.large.png" alt="Implied Relationships - Large" style="max-width: 50%; height: auto; margin: 10px 0;" />
                        <img src="diagrams/summary/relationships.real.compact.png" alt="Real Relationships - Compact" style="max-width: 50%; height: auto; margin: 10px 0;" />
                        <img src="diagrams/summary/relationships.real.large.png" alt="Real Relationships - Large" style="max-width: 50%; height: auto; margin: 10px 0;" />
                    </div>
                """;
                html = html.replaceAll(alertRegex, imageBlock);
                Files.writeString(htmlPath, html);
                System.out.println("Fixed relationships.html successfully.");
            }
        } catch (IOException e) {
            System.err.println("Failed to read relationships.html: " + e.getMessage());
        }

        Path summaryPath = Path.of("visuals/schemaspy/diagrams/summary");
        if(!Files.exists(summaryPath)) {
            System.err.println("Failed to find summary directory: " + summaryPath);
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(summaryPath, "*.dot")) {
            for(Path dotFile : stream) {
                String dotFileName = dotFile.getFileName().toString();
                String outputPng = dotFileName.replace(".dot", ".png");
                String command = String.format(
                    "dot -Tpng:cairo \"%s\" -o \"%s\"",
                    dotFile,
                    summaryPath.resolve(outputPng)
                );

                try {
                    runCommand(command);
                } catch (IOException | InterruptedException e) {
                    System.err.println("Failed to generate PNG for: " + dotFileName + " - " + e.getMessage());
                }

            }
        }  catch (IOException e) {
            System.err.println("Error reading .dot files in summary folder: " + e.getMessage());
        }
    }

    private String getDatabaseName() throws IOException {
        Path configPath = Paths.get("options.json");
        ObjectMapper mapper = new ObjectMapper();

        if (!Files.exists(configPath)) {
            throw new IOException("options.json not found. Run setupInteractive() first.");
        }

        SetupOptions options = mapper.readValue(configPath.toFile(), SetupOptions.class);

        if (options.database == null || options.database.isBlank()) {
            return "main.db"; // Default fallback
        }
        return options.database;
    }

    public void generateSlitePropertiesFile() throws IOException {
        try {
            System.out.println("Generating db.properties...");
            String dbFile = getDatabaseName();
            Path dbPropertiesPath = Paths.get("db.properties");

            // if the db.properties exist ask if user wants to override.
            if (Files.exists(dbPropertiesPath)) {
                System.out.print("db.properties already exists. Override? (yes/no): ");
                String overrideChoice = scanner.nextLine().trim().toLowerCase();
                if (overrideChoice.equals("no")) {
                    System.out.println("Skipping db.properties generation.");
                    return;
                }
            }
    
            String props = String.join("\n",
                "schemaspy.t=sqlite-xerial",
                "schemaspy.dp=tools/sqlite-jdbc-3.49.1.0.jar",
                "schemaspy.db=" + dbFile,
                "schemaspy.s=main",
                "schemaspy.u=ignored",
                "schemaspy.o=visuals/schemaspy",
                "schemaspy.cat=%",
                "schemaspy.url=jdbc:sqlite:" + dbFile,
                "schemaspy.imageformat=svg",
                "schemaspy.renderer=:",
                "schemaspy.fontsize=12",
                "schemaspy.font=Arial",
                "schemaspy.degree=1",
                "schemaspy.maxdet=200"
            );

    
            Files.writeString(dbPropertiesPath, props);
            System.out.println("db.properties file generated successfully.");
    
        } catch (IOException e) {
            System.err.println("Failed to generate db.properties: " + e.getMessage());
            throw e;
        }
    }    

    // .jar files are in ./tools
    public void generateSS() throws IOException, InterruptedException {
        // generate schemaspy
        try {
            System.out.println("Generating SchemaSpy...");
            String dbFile = getDatabaseName();
            Path dbPath = Paths.get(dbFile);
            Path outputDir = Paths.get("./visuals");
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            String command = String.format(
                "java -jar tools/schemaspy-6.2.4.jar -configFile db.properties",
                dbPath.toAbsolutePath().toString()
            );
            runCommand(command);
            System.out.println("SchemaSpy generated successfully.");
        } catch (IOException | InterruptedException e) {
            System.err.println("Failed to generate SchemaSpy: " + e.getMessage());
        }
    }

    private void runCommand(String command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder();

        if (System.getProperty("os.name").contains("Windows")) {
            processBuilder.command("cmd.exe", "/c", command);
        } else {
            processBuilder.command("sh", "-c", command);
        }

        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Command failed with exit code: " + exitCode);
        }

        System.out.println("Command executed successfully: " + command);
    }

    // clears console
    public void clearConsole() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error clearing console: " + e.getMessage());
        }
    }

    public void quit() {
        System.out.println("Goodbye!");
        System.exit(0);
    }
}