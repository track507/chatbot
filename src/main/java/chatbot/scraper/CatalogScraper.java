package chatbot.scraper;
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
import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.io.FileReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Path;

public class CatalogScraper {
    private static final Logger logger = Logger.getLogger(CatalogScraper.class.getName());
    private static final MajorScraper majorScraper = new MajorScraper();
    private static final DepartmentScraper departmentScraper = new DepartmentScraper();
    private static final GenEdScraper genEdScraper = new GenEdScraper();
    private static final CourseScraper courseScraper = new CourseScraper();

    public static String getFirstAvailable(Map<String, String> map, String... keys) {
        for (String key : keys) {
            String value = map.get(key);
            if (value != null) return value;
        }
        return null;
    }

    public void scrapeAll() throws IOException {
        // this will create a headless Chrome browser instance using WebDriverManager
        WebDriverManager.chromedriver().setup();
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--headless");
        WebDriver driver = new ChromeDriver(chromeOptions);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // connect and navigate to the ACU catalog page
        driver.get("https://catalog.acu.edu/");

        // Use hidden select element directly
        WebElement hiddenSelect = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("select_catalog")));
        List<WebElement> optionsList = hiddenSelect.findElements(By.tagName("option"));

        Map<String, Integer> catalogYears = new HashMap<>();

        for (WebElement option : optionsList) {
            String year = option.getText().trim();
            String value = option.getAttribute("value");

            if (value != null && !value.isEmpty()) {
                try {
                    int catoid = Integer.parseInt(value);
                    catalogYears.put(year, catoid);
                    logger.info("Found catalog year: " + year + " with catoid: " + catoid);
                } catch (NumberFormatException ignored) {}
            }
        }

        driver.quit();

        // now loop through each catalog year and its navlinks to srape majors, courses, academic departments, and general education/unversity requirements, 
        // load nav links again to get Academic Programs A-Z URLs
        CatalogUtils utils = new CatalogUtils();
        // save all collected navigation links into JSON
        File dataDir = new File("data");
        if (!dataDir.exists()) dataDir.mkdirs();

        String outputFilePath = "data/catalog_navigation.json";
        utils.saveCatalogNavigationToJson(catalogYears, outputFilePath);
        Map<String, Map<String, Object>> catalogData = utils.loadCatalogData(outputFilePath);
        for (Map.Entry<String, Map<String, Object>> entry : catalogData.entrySet()) {
            String catalogLabel = entry.getKey();
            Map<String, Object> details = entry.getValue();
            int catoid = ((Double) details.get("catoid")).intValue();
            Map<String, String> navLinks = (Map<String, String>) details.get("navLinks");

            // directory named after full catalog label
            String safeLabel = catalogLabel.replaceAll("[^a-zA-Z0-9\\-_ ]", "").replaceAll("\\s+", "_");
            Path catalogDir = new File("data", safeLabel).toPath();
            if (!catalogDir.toFile().exists()) catalogDir.toFile().mkdirs();

            // use navLinks to extract URLs
            String academicProgramsUrl = navLinks.get("Academic Programs A-Z");
            String academicDepartmentsUrl = navLinks.get("Academic Departments");
            String generalEducationUrl = getFirstAvailable(navLinks, "General Education Requirements", "University Requirements");
            String coursesUrl = navLinks.get("Course Descriptions");

            // if (academicProgramsUrl != null) {
            //     majorScraper.scrapeMajorsFromCatalog(catalogLabel, catoid, academicProgramsUrl, catalogDir);
            // }
            // if (academicDepartmentsUrl != null) {
            //     departmentScraper.scrapeDepartmentsFromCatalog(catalogLabel, catoid, academicDepartmentsUrl, catalogDir);
            // }
            // if(generalEducationUrl != null) {
            //     genEdScraper.scrapeGenEdRequirements(catalogLabel, catoid, generalEducationUrl, catalogDir);
            // }
            if(coursesUrl != null) {
                courseScraper.scrapeCoursesFromCatalog(catalogLabel, catoid, coursesUrl, catalogDir);
            }
        }
    }
}
