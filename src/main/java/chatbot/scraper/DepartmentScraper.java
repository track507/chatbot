package chatbot.scraper;
import chatbot.helpers.SetupOptions;
import io.github.bonigarcia.wdm.WebDriverManager;
// https://github.com/SeleniumHQ/selenium/tree/trunk/java
// specifically (https://github.com/SeleniumHQ/selenium/tree/trunk/java/src/org/openqa/selenium)
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
// normal java
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.logging.Logger;

public class DepartmentScraper {
    private static final Logger logger = Logger.getLogger(DepartmentScraper.class.getName());

    public void scrapeDepartmentsFromCatalog(String catalogLabel, int catoid, String departmentsUrl, Path outputDir) {
        logger.info("Scraping departments for: " + catalogLabel + " with URL: " + departmentsUrl);

        SetupOptions options = SetupOptions.load();
        WebDriverManager.chromedriver().setup();
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--headless=new");
        WebDriver driver = new ChromeDriver(chromeOptions);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        StringBuilder fullText = new StringBuilder();
        fullText.append("Catalog: ").append(catalogLabel).append("\n");
        fullText.append("URL: ").append(departmentsUrl).append("\n\n");

        try {
            driver.get(departmentsUrl);

            // wait for the table structure to load
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("td.block_content table.table_default")));

            WebElement outerTd = driver.findElement(By.cssSelector("td.block_content"));
            List<WebElement> tableTds = outerTd.findElements(By.cssSelector("table.table_default > tbody > tr > td"));

            WebElement targetTd = null;
            for (WebElement td : tableTds) {
                if (!td.findElements(By.tagName("ul")).isEmpty() && !td.findElements(By.tagName("p")).isEmpty()) {
                    targetTd = td;
                    break;
                }
            }

            if (targetTd == null) {
                throw new NoSuchElementException("Could not locate target <td> with <ul> and <p>");
            }

            List<WebElement> elements = targetTd.findElements(By.xpath("./*"));
            String currentCollege = null;

            for (WebElement el : elements) {
                String tag = el.getTagName();
                if ("p".equals(tag)) {
                    try {
                        WebElement strong = el.findElement(By.tagName("strong"));
                        currentCollege = strong.getText().trim();
                        fullText.append(currentCollege).append("\n\n");
                        logger.info("Found college: " + currentCollege);
                    } catch (NoSuchElementException ignored) {}
                } else if ("ul".equals(tag)) {
                    List<WebElement> departments = el.findElements(By.tagName("li"));
                    for (WebElement dept : departments) {
                        try {
                            String deptName = dept.getText().trim();
                            logger.info("  Found department: " + deptName);
                            fullText.append("  ").append(deptName).append("\n");
                        } catch (Exception ignored) {}
                    }
                    fullText.append("\n");
                }
            }

        } catch (Exception e) {
            logger.warning("Error scraping departments: " + e.getMessage());
        } finally {
            driver.quit();
        }

        // write to file only if content seems meaningful (no pesty freeloaders)
        String fileName = "departments_" + catalogLabel.replaceAll("[^a-zA-Z0-9]", "_") + ".txt";
        File file = outputDir.resolve(fileName).toFile();

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(fullText.toString());
            logger.info("Saved departments to: " + file.getAbsolutePath());
        } catch (IOException e) {
            logger.warning("Failed to write departments file: " + e.getMessage());
        }
    }
}
