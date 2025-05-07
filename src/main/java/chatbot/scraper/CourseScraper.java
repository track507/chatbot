package chatbot.scraper;
import chatbot.helpers.SetupOptions;
// https://github.com/bonigarcia/webdrivermanager
import io.github.bonigarcia.wdm.WebDriverManager;
// https://github.com/SeleniumHQ/selenium/tree/trunk/java
// specifically (https://github.com/SeleniumHQ/selenium/tree/trunk/java/src/org/openqa/selenium)
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
// boringggg
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.logging.Logger;

public class CourseScraper {
    private static final Logger logger = Logger.getLogger(CourseScraper.class.getName());

    public void scrapeCoursesFromCatalog(String catalogLabel, int catoid, String courseUrl, Path outputDir) {
        logger.info("Scraping courses for: " + catalogLabel + " from URL: " + courseUrl);

        SetupOptions options = SetupOptions.load();
        WebDriverManager.chromedriver().setup();
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--headless=new");
        WebDriver driver = new ChromeDriver(chromeOptions);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        StringBuilder fullText = new StringBuilder();
        fullText.append("Catalog: ").append(catalogLabel).append("\n");
        fullText.append("URL: ").append(courseUrl).append("\n\n");

        try {
            String nextUrl = courseUrl;

            while (nextUrl != null) {
                driver.get(nextUrl);
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("td.block_content > table.table_default")));

                List<WebElement> tables = driver.findElements(By.cssSelector("td.block_content > table.table_default"));
                WebElement contentTable = tables.get(tables.size() - 1);

                List<WebElement> allRows = contentTable.findElements(By.cssSelector("tbody > tr"));
                String currentDepartment = null;

                for (WebElement row : allRows) {
                    List<WebElement> tds = row.findElements(By.tagName("td"));
                    for (WebElement td : tds) {
                        try {
                            if ("2".equals(td.getAttribute("colspan"))) {
                                WebElement strong = td.findElement(By.cssSelector("p > strong"));
                                currentDepartment = strong.getText().trim();
                                fullText.append(currentDepartment).append("\n");
                                logger.info("Found department: " + currentDepartment);
                                continue;
                            }
                        } catch (NoSuchElementException ignored) {}

                        if (td.getAttribute("class") != null && td.getAttribute("class").contains("width")) {
                            List<WebElement> courseLinks = td.findElements(By.tagName("a"));
                            for (WebElement course : courseLinks) {
                                String courseTitle = course.getText().trim();
                                fullText.append("  ").append(courseTitle).append("\n");
                                logger.info("  Found course: " + courseTitle);
                            }
                        }
                    }
                }

                // Find next page URL
                nextUrl = null;
                List<WebElement> nextLinks = driver.findElements(By.cssSelector("td[colspan='2'] > a"));
                for (WebElement a : nextLinks) {
                    String label = a.getText().toLowerCase().trim();
                    if (label.equals("forward 10") || label.matches("\\d+")) {
                        nextUrl = a.getAttribute("href");
                    }
                }
            }

        } catch (Exception e) {
            logger.warning("Error scraping courses: " + e.getMessage());
        } finally {
            driver.quit();
        }

        String fileName = "courses_" + catalogLabel.replaceAll("[^a-zA-Z0-9]", "_") + ".txt";
        File file = outputDir.resolve(fileName).toFile();

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(fullText.toString());
            logger.info("Saved courses to: " + file.getAbsolutePath());
        } catch (IOException e) {
            logger.warning("Failed to write courses file: " + e.getMessage());
        }
    }
}