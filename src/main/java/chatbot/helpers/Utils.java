package chatbot.helpers;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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