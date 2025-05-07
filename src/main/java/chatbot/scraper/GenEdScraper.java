package chatbot.scraper;

import chatbot.helpers.SetupOptions;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.logging.Logger;

public class GenEdScraper {
    private static final Logger logger = Logger.getLogger(GenEdScraper.class.getName());

    public void scrapeGenEdRequirements(String programLabel, int catoid, String programUrl, Path outputDir) {
        logger.info("Scraping Gen Ed for: " + programLabel + " from URL: " + programUrl);

        SetupOptions options = SetupOptions.load();
        WebDriverManager.chromedriver().setup();
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--headless=new");
        WebDriver driver = new ChromeDriver(chromeOptions);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        StringBuilder fullText = new StringBuilder();
        fullText.append("Program: ").append(programLabel).append("\n");
        fullText.append("URL: ").append(programUrl).append("\n\n");

        try {
            driver.get(programUrl);

            // wait for the custom content container
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.custom_leftpad_20")));

            List<WebElement> leftpadDivs = driver.findElements(By.cssSelector("div.custom_leftpad_20"));
            for (WebElement container : leftpadDivs) {
                List<WebElement> acalogDivs = container.findElements(By.cssSelector("div.acalog-core"));
                for (WebElement core : acalogDivs) {
                    String text = core.getText().trim();
                    if (!text.isEmpty()) {
                        fullText.append(text).append("\n\n");
                        logger.info("Extracted acalog-core block with length: " + text.length());
                    }
                }
            }

        } catch (Exception e) {
            logger.warning("Error scraping Gen Ed requirements: " + e.getMessage());
        } finally {
            driver.quit();
        }

        // write to file
        String fileName = "gened_required_" + programLabel.replaceAll("[^a-zA-Z0-9]", "_") + ".txt";
        File file = outputDir.resolve(fileName).toFile();

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(fullText.toString());
            logger.info("Saved Gen Ed content to: " + file.getAbsolutePath());
        } catch (IOException e) {
            logger.warning("Failed to write Gen Ed file: " + e.getMessage());
        }
    }
}
