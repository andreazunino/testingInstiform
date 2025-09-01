package qa.andrea.tests;

import org.testng.annotations.Test;
import qa.andrea.core.BaseTest;

public class LoginTest extends BaseTest {

    @Test
    public void canOpenHomepage() {
        driver().get("https://example.org/");
        // Fuerza un assert simple (adaptá a tu app)
        assert driver().getTitle() != null;
    }
}

