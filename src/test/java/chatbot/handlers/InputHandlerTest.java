package chatbot.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import chatbot.models.RequestPayload;
public class InputHandlerTest {

    @Test
    public void testHandleInputValid() {
        InputHandler handler = new InputHandler();
        RequestPayload result = handler.handleInput("Hello world");
        assertEquals("Hello world", result.getContent());
        assertEquals("user", result.getRole());
    }

    @Test
    public void testHandleInputEmptyThrowsException() {
        InputHandler handler = new InputHandler();
        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            handler.handleInput("   ");
        });
        assertEquals("Input cannot be empty.", ex.getMessage());
    }
}
