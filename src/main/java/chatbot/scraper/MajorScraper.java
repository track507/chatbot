package chatbot.scraper;
import chatbot.helpers.SetupOptions;
// https://github.com/SeleniumHQ/selenium/tree/trunk/java
// specifically (https://github.com/SeleniumHQ/selenium/tree/trunk/java/src/org/openqa/selenium)
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.TimeoutException;
// https://github.com/bonigarcia/webdrivermanager
import io.github.bonigarcia.wdm.WebDriverManager;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.logging.Logger;
import java.util.ArrayList;

public class MajorScraper {
    private static final Logger logger = Logger.getLogger(MajorScraper.class.getName());

    public void scrapeMajorsFromCatalog(String year, int catoid, String academicProgramsUrl, Path outputDir) {
        logger.info("Scraping majors for year: " + year + " using URL: " + academicProgramsUrl);

        SetupOptions options = SetupOptions.load();
        WebDriverManager.chromedriver().setup();
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--headless");
        WebDriver driver = new ChromeDriver(chromeOptions);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        StringBuilder fullText = new StringBuilder();
        fullText.append("Catalog Year: ").append(year).append("\n");

        try {
            driver.get(academicProgramsUrl);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("ul.program-list")));

            // extract text/href now
            List<WebElement> majorElements = driver.findElements(By.cssSelector("ul.program-list li a"));
            List<String> majorNames = new ArrayList<>();
            List<String> majorUrls = new ArrayList<>();

            for (WebElement major : majorElements) {
                majorNames.add(major.getText().trim());
                majorUrls.add(major.getAttribute("href"));
            }

            for (int i = 0; i < majorUrls.size(); i++) {
                String majorName = majorNames.get(i);
                String majorUrl = majorUrls.get(i);
                logger.info("Scraping major: " + majorName);

                try {
                    driver.get(majorUrl);
                    List<WebElement> courseSections = wait.until(
                        ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("div.acalog-core"))
                    );

                    fullText.append("\nMajor: ").append(majorName).append("\n");
                    fullText.append("URL: ").append(majorUrl).append("\n");

                    for (WebElement section : courseSections) {
                        fullText.append(section.getText()).append("\n");
                    }
                } catch (TimeoutException e) {
                    logger.warning("Timed out scraping major: " + majorName + " → " + majorUrl);
                }

                // revisit the main listing to prep for the next scrape
                driver.get(academicProgramsUrl);
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("ul.program-list")));
            }

        } catch (Exception e) {
            logger.warning("Unexpected error during major scraping: " + e.getMessage());
            e.printStackTrace();
        } finally {
            driver.quit();
        }

        // save content to file
        String fileName = "majors_" + year.replaceAll("[^a-zA-Z0-9]", "_") + ".txt";
        File file = outputDir.resolve(fileName).toFile();

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(fullText.toString());
            logger.info("Saved major content to: " + file.getAbsolutePath());
        } catch (IOException e) {
            logger.warning("Failed to save majors file: " + e.getMessage());
            return;
        }
    }
}