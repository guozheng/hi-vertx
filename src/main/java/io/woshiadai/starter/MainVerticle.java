package io.woshiadai.starter;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class MainVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        startHttpServer();
    }

    private Future<Void> startHttpServer() {
        Future<Void> future = Future.future();
        HttpServer server = vertx.createHttpServer(new HttpServerOptions()
            .setSsl(true)
            .setKeyStoreOptions(new JksOptions()
                .setPath("server-keystore.jks")
                .setPassword("secret"))
        );

        Router router = Router.router(vertx);
        router.get("/").handler(this::getRoot);

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

    private void getRoot(RoutingContext ctx) {
        HttpServerResponse resp = (HttpServerResponse) ctx.response();
        resp.putHeader("Content-Type", "text/plain");
        resp.end("Hello from Vert.x!");
    }

}
