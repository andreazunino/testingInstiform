package qa.andrea.core;

import com.epam.healenium.SelfHealingDriver;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

public abstract class BaseTest {

    private static final ThreadLocal<SelfHealingDriver> TL_DRIVER = new ThreadLocal<>();
    protected Path artifactsBaseDir;

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception {
        WebDriverManager.chromedriver().setup();

        LoggingPreferences logPrefs = new LoggingPreferences();
        logPrefs.enable(LogType.BROWSER, Level.ALL);

        ChromeOptions options = new ChromeOptions();
        options.setCapability("goog:loggingPrefs", logPrefs);
        if ("true".equalsIgnoreCase(System.getProperty("CI", System.getenv("CI")))) {
            options.addArguments("--headless=new", "--disable-gpu", "--window-size=1920,1080");
        }

        ChromeDriver delegate = new ChromeDriver(options);
        SelfHealingDriver driver = SelfHealingDriver.create(delegate);
        TL_DRIVER.set(driver);

        artifactsBaseDir = Path.of("artifacts");
        Files.createDirectories(artifactsBaseDir);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        SelfHealingDriver d = TL_DRIVER.get();
        if (d != null) {
            try { d.quit(); } catch (Exception ignored) {}
            TL_DRIVER.remove();
        }
    }

    protected SelfHealingDriver driver() {
        return TL_DRIVER.get();
    }
}

