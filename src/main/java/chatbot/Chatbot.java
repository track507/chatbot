package chatbot;
import java.sql.SQLException;
import java.util.Scanner;

import chatbot.helpers.Utils;
import chatbot.llm.Embedder;
import chatbot.llm.EmbeddingException;
import chatbot.llm.LLMQuery;
import chatbot.llm.OllamaEngine;
import chatbot.llm.QdrantQuery;

// TODO:
/*
    - Implement method to parse user input
    - Method to convert input into JSON object
    - Method to send JSON object to local ollama server
    - Method to receive response from ollama server
    - Method to parse response into JSON object to display to user
    - Method to display response to user
    - Method for formatting options e.g. text color, background color, text enginge, etc.
    - Method to handle errors and exceptions

    ! Some of these methods may be implemented in separate classes
*/

public class Chatbot {
    private final Utils utils = new Utils();

    public void start() {

        try {
            utils.setupInteractive();
            utils.clearConsole();
        } catch (EmbeddingException e) {
            System.err.println("Embedding Error during setup: " + e.getMessage());
            e.printStackTrace();
            return; 
        } catch (Exception e) {
            System.err.println("Unexpected error during setup: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        try {
            utils.login();
        } catch (SQLException e) {
            System.err.println("Database error during login: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid credentials: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error during login: " + e.getMessage());
        }

        utils.clearConsole();

        System.out.println("Welcome to Chatbot!");
        System.out.println("Type 'exit' or 'quit' to quit or 'reset' to restart the conversation.");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                Embedder embedder = new Embedder();
                OllamaEngine ollama = new OllamaEngine();
                QdrantQuery qdrant = new QdrantQuery();
                LLMQuery engine = new LLMQuery(embedder, qdrant);

                String input;
                while (true) { 
                    try {
                        System.out.print("> ");
                        input = scanner.nextLine().trim();

                        if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                            utils.quit();
                        }

                        if (input.equalsIgnoreCase("reset")) {
                            ollama.resetConversation();
                            System.out.println("Conversation reset.");
                            continue;
                        }

                        String response = engine.ask(input);
                        System.out.println("\nAdvisor: " + response);

                    } catch (Exception e) {
                        System.err.println("Error: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Chatbot chatbot = new Chatbot();
        chatbot.start();
    }
}
