package chatbot.scraper;
import chatbot.langchain.LangchainEmbed;
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
// https://github.com/bonigarcia/webdrivermanager
import io.github.bonigarcia.wdm.WebDriverManager;
// https://github.com/google/gson
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.io.FileReader;
import java.io.Reader;

public class CatalogUtils {
    private static final Logger logger = Logger.getLogger(CatalogUtils.class.getName());

    public Map<String, String> getNavLinks(int catoid) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--headless");
        WebDriver driver = new ChromeDriver(chromeOptions);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        String url = "https://catalog.acu.edu/index.php?catoid=" + catoid;
        driver.get(url);
        //debug
        //logger.info("Navigating to: " + url);
        Map<String, String> navLinks = new HashMap<>();

        //debug
        //logger.info("Attempting to find navigation items...");
        WebElement navTable = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("acalog-navigation"))
        );
        List<WebElement> navItems = navTable.findElements(By.cssSelector("a.navbar"));

        for (WebElement item : navItems) {
            String label = item.getText().trim();
            String href = item.getAttribute("href");
            if (!label.isEmpty() && href != null) {
                navLinks.put(label, href);
                //logger.info("Found nav link: " + label + " → " + href);
            }
        }

        driver.quit();
        return navLinks;
    }

    public void saveCatalogNavigationToJson(Map<String, Integer> catalogYears, String outputFilePath) {
        Map<String, Object> result = new HashMap<>();

        for (Map.Entry<String, Integer> entry : catalogYears.entrySet()) {
            String year = entry.getKey();
            int catoid = entry.getValue();

            Map<String, String> navLinks = getNavLinks(catoid);
            Map<String, Object> catalogEntry = new HashMap<>();
            catalogEntry.put("catoid", catoid);
            catalogEntry.put("url", "https://catalog.acu.edu/index.php?catoid=" + catoid);
            catalogEntry.put("navLinks", navLinks);
            result.put(year, catalogEntry);
        }

        Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(new File(outputFilePath))) {
            gson.toJson(result, writer);
            logger.info("Navigation links saved to: " + outputFilePath);
        } catch (IOException e) {
            logger.warning("Failed to save catalog navigation JSON: " + e.getMessage());
        }
    }

    public static Map<String, Map<String, Object>> loadCatalogData(String jsonPath) throws IOException {
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Map<String, Object>>>(){}.getType();
        try (Reader reader = new FileReader(jsonPath)) {
            return gson.fromJson(reader, type);
        }
    }
} 
