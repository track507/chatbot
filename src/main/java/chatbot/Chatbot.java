package chatbot;
import java.sql.SQLException;
import java.util.Scanner;

import chatbot.helpers.Utils;
import chatbot.llm.EmbeddingException;

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
            utils.login();
        } catch (SQLException e) {
            System.err.println("Database error during login: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid credentials: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error during login: " + e.getMessage());
        }

        try {
            utils.setupInteractive(); 
        } catch (EmbeddingException e) {
            System.err.println("Embedding Error during setup: " + e.getMessage());
            e.printStackTrace();
            return; 
        } catch (Exception e) {
            System.err.println("Unexpected error during setup: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // try starting the ollama LLM engine
        // try {
            
        // } catch ( e) {
        // }

        Scanner scanner = new Scanner(System.in);
        String input;
        utils.clearConsole();
        System.out.println("Welcome to Chatbot!");
        while (true) {
            try {
                System.out.print("> ");
                input = scanner.nextLine().trim().toLowerCase();
        
                if (input.equals("exit") || input.equals("quit")) {
                    scanner.close();
                    utils.quit();
                }
                // OllamaEngine.java is not yet finished and implemented.
                // TODO: Finish this
        
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
        
    }

    public static void main(String[] args) {
        Chatbot chatbot = new Chatbot();
        chatbot.start();
    }
}
