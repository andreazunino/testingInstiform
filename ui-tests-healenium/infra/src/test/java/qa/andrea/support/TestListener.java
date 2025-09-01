package qa.andrea.support;

import com.epam.healenium.SelfHealingDriver;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.testng.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TestListener implements ITestListener {

    private Path ensureCaseDir(ITestResult result) throws Exception {
        String className = result.getTestClass().getName();
        String methodName = result.getMethod().getMethodName();
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
        Path dir = Path.of("artifacts", className + "__" + methodName + "__" + timestamp);
        Files.createDirectories(dir);
        return dir;
    }

    @Override
    public void onTestFailure(ITestResult result) {
        Object instance = result.getInstance();
        try {
            var m = instance.getClass().getSuperclass().getDeclaredMethod("driver");
            m.setAccessible(true);
            SelfHealingDriver driver = (SelfHealingDriver) m.invoke(instance);

            Path caseDir = ensureCaseDir(result);

            Files.writeString(caseDir.resolve("DOM.html"), driver.getPageSource(), StandardCharsets.UTF_8);

            File shot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(shot, caseDir.resolve("screenshot.png").toFile());

            List<LogEntry> logs = driver.manage().logs().get(LogType.BROWSER).getAll();
            StringBuilder sb = new StringBuilder();
            logs.forEach(e -> sb.append("[").append(e.getLevel()).append("] ")
                    .append(e.getTimestamp()).append(" ")
                    .append(e.getMessage()).append("\n"));
            Files.writeString(caseDir.resolve("browser.log"), sb.toString(), StandardCharsets.UTF_8);

            if ("true".equalsIgnoreCase(System.getenv().getOrDefault("JIRA_AUTO_CREATE", "false"))) {
                String summary = "UI test failed: " + result.getTestClass().getName() + "#" + result.getMethod().getMethodName();
                String description = "Fallo detectado automáticamente. Se adjuntan DOM, screenshot y logs.";
                String issueKey = JiraHelper.createIssue(summary, description);
                JiraHelper.attachFile(issueKey, caseDir.resolve("DOM.html").toFile());
                JiraHelper.attachFile(issueKey, caseDir.resolve("screenshot.png").toFile());
                JiraHelper.attachFile(issueKey, caseDir.resolve("browser.log").toFile());
            }

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override public void onTestStart(ITestResult result) {}
    @Override public void onTestSuccess(ITestResult result) {}
    @Override public void onTestSkipped(ITestResult result) {}
    @Override public void onTestFailedButWithinSuccessPercentage(ITestResult result) {}
    @Override public void onStart(ITestContext context) {}
    @Override public void onFinish(ITestContext context) {}
}
