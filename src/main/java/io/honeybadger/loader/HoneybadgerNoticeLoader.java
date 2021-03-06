package io.honeybadger.loader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.honeybadger.reporter.HoneybadgerExclusionStrategy;
import io.honeybadger.reporter.config.ConfigContext;
import io.honeybadger.reporter.dto.Notice;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.util.UUID;

/**
 * Utility class used to load a fault's details into a readable object
 * structure.
 *
 * @author <a href="https://github.com/dekobon">Elijah Zupancic</a>
 * @since 1.0.9
 */
public class HoneybadgerNoticeLoader {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Gson gson = new GsonBuilder()
            .setExclusionStrategies(new HoneybadgerExclusionStrategy())
            .create();
    private ConfigContext config;

    public HoneybadgerNoticeLoader(ConfigContext config) {
        this.config = config;
    }

    String pullFaultJson(UUID faultId) throws IOException {
        String readApiKey = config.getHoneybadgerReadApiKey();

        if (readApiKey == null) {
            String msg = "Read API key must be set";
            throw new IllegalArgumentException(msg);
        }

        final URI baseURI = URI.create(String.format("%s/%s/%s",
                config.getHoneybadgerUrl(), "v1/notices", faultId));

        String withAuth = String.format("%s/?auth_token=%s",
                baseURI, readApiKey);

        logger.debug("Querying for error details: {}", baseURI);

        Response response;
        HttpResponse httpResponse = null;

        // We loop here because the API returns 404 when the notice still
        // hasn't finished processing
        for (int i = 0; i < 3; i++) {
            response = Request.Get(withAuth)
                              .addHeader("Accept", "application/json")
                              .execute();

            httpResponse = response.returnResponse();

            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                break;
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                break;
            }

            if  (i == 2) {
                throw new IllegalArgumentException("Unable to get notice from API");
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        httpResponse.getEntity().writeTo(out);

        return out.toString();
    }

    public Notice findErrorDetails(UUID faultId) throws IOException {
        String json = pullFaultJson(faultId);

        // HACK: Since our API is not symmetric, we do this in order to rename fields
        // and get *some* of the data that we sent.
        JsonObject originalJson = gson.fromJson(json, JsonObject.class).getAsJsonObject();
        JsonObject cgiData = originalJson.get("web_environment").getAsJsonObject();
        originalJson.get("request").getAsJsonObject().add("cgi_data", cgiData);

        Notice error;

        try {
            ConfigContext.threadLocal.set(config);
            error = gson.fromJson(originalJson, Notice.class);
        } finally {
            ConfigContext.threadLocal.remove();
        }

        return error;
    }
}
