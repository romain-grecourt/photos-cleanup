package com.example.photocleanup;

import io.helidon.security.SecurityContext;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import io.helidon.security.integration.webserver.WebSecurity;
import io.helidon.security.providers.common.TokenCredential;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonValue;

final class PhotoService implements Service {

    private static final JsonReaderFactory JSON = Json.createReaderFactory(Collections.emptyMap());

    PhotoService() {
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/list", WebSecurity.authenticate(), this::listAlbums);
    }

    private void listAlbums(ServerRequest req, ServerResponse res) {
        try {
            res.send(getAlbumNames(getAccessToken(req)));
        } catch(IOException ex) {
            req.next(ex);
        }
    }

    private static String getAccessToken(ServerRequest req) {
        return req.queryParams().first("access_token").orElse(null);
    }

    private static String getIdToken(ServerRequest req) {
        return req.context().get(SecurityContext.class)
                .flatMap(SecurityContext::user)
                .flatMap(s -> s.publicCredential(TokenCredential.class))
                .map(tc -> tc.token())
                .orElseThrow(() -> new IllegalStateException("Unable to get id token"));
    }

    private static String getAlbumNames(String accessToken) throws IOException {
        URL url = new URL("https://photoslibrary.googleapis.com/v1/albums");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setRequestProperty("Accept", "application/json");
        JsonReader jsonReader = JSON.createReader(conn.getInputStream());
        JsonObject jsonObject = jsonReader.readObject();
        JsonArray albums = jsonObject.getJsonArray("albums");
        StringBuilder sb = new StringBuilder();
        for(JsonValue album : albums) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(album.asJsonObject().getString("title"));
        }
        return sb.toString();
    }
}
