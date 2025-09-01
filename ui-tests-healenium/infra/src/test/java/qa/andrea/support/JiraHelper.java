package qa.andrea.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class JiraHelper {

    private static final String BASE = System.getenv("JIRA_BASE_URL");
    private static final String EMAIL = System.getenv("JIRA_USER_EMAIL");
    private static final String TOKEN = System.getenv("JIRA_API_TOKEN");
    private static final String PROJECT = System.getenv("JIRA_PROJECT_KEY");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static String authHeader() {
        String basic = EMAIL + ":" + TOKEN;
        return "Basic " + Base64.getEncoder().encodeToString(basic.getBytes(StandardCharsets.UTF_8));
    }

    public static String createIssue(String summary, String descriptionPlain) throws Exception {
        Map<String, Object> adf = Map.of(
            "type", "doc",
            "version", 1,
            "content", new Object[]{ Map.of("type","paragraph","content",
                new Object[]{ Map.of("type","text","text", descriptionPlain) }) }
        );

        Map<String, Object> body = Map.of(
            "fields", Map.of(
                "project", Map.of("key", PROJECT),
                "summary", summary,
                "issuetype", Map.of("name", "Bug"),
                "description", adf
            )
        );

        String url = BASE + "/rest/api/3/issue";
        HttpPost post = new HttpPost(url);
        post.addHeader("Authorization", authHeader());
        post.addHeader("Accept", "application/json");
        post.addHeader("Content-Type", "application/json");
        post.setEntity(new StringEntity(MAPPER.writeValueAsString(body), ContentType.APPLICATION_JSON));

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            var res = client.execute(post);
            var json = MAPPER.readTree(res.getEntity().getContent());
            return json.get("key").asText();
        }
    }

    public static void attachFile(String issueKey, File file) throws Exception {
        String url = BASE + "/rest/api/3/issue/" + issueKey + "/attachments";
        HttpUriRequestBase post = new HttpPost(url);
        post.addHeader("Authorization", authHeader());
        post.addHeader("X-Atlassian-Token", "no-check");
        post.addHeader("Accept", "application/json");

        MultipartEntityBuilder mb = MultipartEntityBuilder.create()
            .addBinaryBody("file", file, ContentType.DEFAULT_BINARY, file.getName());
        HttpEntity entity = mb.build();
        post.setEntity(entity);

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            client.execute(post).close();
        }
    }
}
