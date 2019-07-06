package io.woshiadai.starter;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.*;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class MainVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start(Future<Void> startFuture) {
        startHttpServer();
        LOGGER.info("Server started...");
    }

    private Future<Void> startHttpServer() {
        Future<Void> future = Future.future();
        HttpServer server = vertx.createHttpServer(
//                new HttpServerOptions()
//            .setSsl(true)
//            .setKeyStoreOptions(new JksOptions()
//                .setPath("server-keystore.jks")
//                .setPassword("secret"))
        );

        Router router = Router.router(vertx);
        router.get("/google").handler(this::makeRequest);

        server.requestHandler(router)
            .listen(8888, http -> {
                if (http.succeeded()) {
                    LOGGER.info("HTTP server started and running on port 8888");
                    future.complete();
                } else {
                    LOGGER.info("Could not start HTTP server", http.cause());
                    future.fail(http.cause());
                }
            });

        return future;
    }

    private void makeRequest(RoutingContext ctx) {
        HttpServerResponse serverResp = ctx.response();

        HttpClientOptions options = new HttpClientOptions()
                .setProtocolVersion(HttpVersion.HTTP_2)
                .setSsl(true)
                .setUseAlpn(true)
                .setTrustAll(true);
        HttpClient client = vertx.createHttpClient(options);

        HttpClientRequest req = client.get(443, "www.google.com", "/");

        req.handler(resp -> {
            LOGGER.info("Got response: " + resp.statusCode());
            serverResp.putHeader("Content-Type", "text/html; charset=ISO-8859-1")
                    .setStatusCode(200);
            resp.bodyHandler(buf -> serverResp.end(buf.toString()));
        });

        req.exceptionHandler(err -> {
            LOGGER.error("Error: {}", err);
            serverResp.putHeader("Content-Type", "text/plain")
                    .setStatusCode(500)
                    .end();

        });

        req.end();
    }

}
