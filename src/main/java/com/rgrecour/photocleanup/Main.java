/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.rgrecour.photocleanup;

import io.helidon.config.Config;
import io.helidon.security.Security;
import io.helidon.security.integration.webserver.WebSecurity;
import io.helidon.security.providers.google.login.GoogleTokenProvider;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerConfiguration;
import io.helidon.webserver.StaticContentSupport;
import io.helidon.webserver.WebServer;
import java.io.IOException;
import java.util.logging.LogManager;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws IOException {

        // logging
        LogManager.getLogManager().readConfiguration(Main.class.getResourceAsStream("/logging.properties"));

        // config
        Config config = Config.create();

        // security
        Security security = Security.builder()
                .addProvider(GoogleTokenProvider.builder()
                        .clientId(config.get("google-client-id").asString().get()))
                .build();
        WebSecurity webSecurity = WebSecurity.create(security);

        // routes
        Routing.Builder routing = Routing.builder()
                .register(webSecurity)
                .register("/api", new PhotoService(
                        config.get("google-client-id").asString().get(),
                        config.get("google-client-secret").asString().get()))
                .register(StaticContentSupport.builder("/static")
                        .welcomeFileName("index.html")
                        .build());

        WebServer server = WebServer.create(ServerConfiguration.builder()
                .port(8080),
                routing);
        server.start().thenAccept(ws -> {
            System.out.println(
                    "WEB server is up! http://localhost:" + ws.port());
        });
    }
}
