package qa.andrea.tests;

import org.testng.Assert;
import org.testng.annotations.Test;
import qa.andrea.core.BaseTest;

public class LoginTest extends BaseTest {
    @Test
    public void canOpenHomepage() {
        driver().get("https://example.org/");
        Assert.assertNotNull(driver().getTitle());
    }
}
