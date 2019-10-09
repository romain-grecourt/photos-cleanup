package com.rgrecour.photocleanup;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.UserCredentials;
import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.library.v1.PhotosLibrarySettings;
import com.google.photos.library.v1.internal.InternalPhotosLibraryClient.ListAlbumsPage;
import com.google.photos.library.v1.internal.InternalPhotosLibraryClient.ListAlbumsPagedResponse;
import com.google.photos.types.proto.Album;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import io.helidon.security.integration.webserver.WebSecurity;
import java.io.IOException;


final class PhotoService implements Service {

    private final String clientId;
    private final String clientSecret;

    PhotoService(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/list", WebSecurity.authenticate(), this::listAlbums);
    }

    private void listAlbums(ServerRequest req, ServerResponse res) {
        try {
            PhotosLibraryClient plc = createClient(req);
            res.send(getAlbumNames(plc));
        } catch(IOException ex) {
            req.next(ex);
        }
    }

    private static String getAccessToken(ServerRequest req) {
        return req.queryParams().first("access_token").orElse(null);
    }

    private PhotosLibraryClient createClient(ServerRequest req) throws IOException {
        PhotosLibrarySettings settings = PhotosLibrarySettings.newBuilder()
            .setCredentialsProvider(FixedCredentialsProvider.create(UserCredentials.newBuilder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setAccessToken(new AccessToken(getAccessToken(req), null))
                .build()))
            .build();
        return PhotosLibraryClient.initialize(settings);
    }

    private static String getAlbumNames(PhotosLibraryClient plc) {
        StringBuilder sb = new StringBuilder();
        ListAlbumsPagedResponse lapr = plc.listAlbums();
        for (ListAlbumsPage lap : lapr.iteratePages()) {
            for (Album album : lap.getResponse().getAlbumsList()) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(album.getTitle());
            }
        }
        return sb.toString();
    }
}
