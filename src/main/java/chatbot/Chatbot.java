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
package chatbot;
import chatbot.helpers.SetupOptions;
import chatbot.helpers.Utils;
import chatbot.langchain.EmbeddingException;
import chatbot.langchain.LangchainEmbed;
import chatbot.langchain.LangchainQuery;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

public class Chatbot {

    private final Utils utils = new Utils();
    private LangchainQuery langchainQuery;

    public void start() {

        SetupOptions options;
        
        try {
            utils.setupInteractive();
            utils.clearConsole();
            options = SetupOptions.load();
            langchainQuery = new LangchainQuery(options);
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
            return;
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid credentials: " + e.getMessage());
            return;
        }

        utils.clearConsole();

        System.out.println("Welcome to Chatbot!");
        System.out.println("Type 'exit' or 'quit' to quit or 'reset' to restart the conversation.");

        try (Scanner scanner = new Scanner(System.in)) {

            while (true) {
                String userProfile = utils.formatUserProfile(utils.loadUserInfo());
                System.out.print("> ");
                String input = scanner.nextLine().trim();

                if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                    utils.quit();
                }

                if (input.equalsIgnoreCase("reset")) {
                    System.out.println("Conversation reset.");
                    langchainQuery.getHistory().clear();
                    continue;
                }

                if (!input.isEmpty()) {
                    System.out.println("\nAdvisor:");
                    CountDownLatch latch = new CountDownLatch(1);
                    langchainQuery.streamQuery(
                        input,
                        userProfile,
                        token -> System.out.print(token),
                        response -> {
                            System.out.println("\n");
                            latch.countDown(); // Allow next user input
                        }
                    );

                    try {
                        latch.await(); // Block until response is done
                    } catch (InterruptedException e) {
                        System.err.println("Interrupted while waiting for chatbot response.");
                        Thread.currentThread().interrupt();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error during conversation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new Chatbot().start();
    }
}