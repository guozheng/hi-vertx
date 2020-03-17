package io.woshiadai.starter;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.http.*;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start(Future<Void> startFuture) {
        // set vertx logger delegate factory to slf4j
        String logFactory = System.getProperty("org.vertx.logger-delegate-factory-class-name");
        if (logFactory == null) {
            System.setProperty("org.vertx.logger-delegate-factory-class-name",
                    SLF4JLogDelegateFactory.class.getName());
        }

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

        // set router options
        router.route().handler(BodyHandler.create().setBodyLimit(10 * 1024 * 1024)); // 10MB max body size
        router.route().handler(ResponseTimeHandler.create()); // add a response header: x-response-time: xyzms
        router.route().handler(TimeoutHandler.create(500)); // request timeout in ms
        router.route().failureHandler(ErrorHandler.create(false)); // no exception details

        // use customized request logger
        // there are three logger format: DEFAULT, SHORT, TINY, see Slf4jRequestLogger.java for details
        // you can make it configurable, e.g. dev using DEFAULT, prod using TINY
        LoggerFormat loggerFormat = LoggerFormat.DEFAULT;
        router.route().handler(RequestLogHandler.create(loggerFormat));

        // set routes and handlers
        router.get("/postman").handler(this::getPostmanEcho);
        router.post("/postman").handler(this::postPostmanEcho);

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

    /**
     * GET request to postman echo service, see: https://docs.postman-echo.com/?version=latest
     *
     * @param ctx {@link RoutingContext} context object
     */
    private void getPostmanEcho(RoutingContext ctx) {
        HttpClientOptions options = new HttpClientOptions()
                .setProtocolVersion(HttpVersion.HTTP_2)
                .setSsl(true)
                .setUseAlpn(true)
                .setTrustAll(true);
        HttpClient client = vertx.createHttpClient(options);

        HttpClientRequest req = client.get(443, "postman-echo.com", "/get?foo1=bar1&foo2=bar2")
                .setTimeout(2000);
        MultiMap headers = req.headers();
        headers.set("Accept", "application/json");

        HttpServerResponse serverResp = ctx.response();

        req.handler(resp -> {
            LOGGER.info("Response from postman echo: {}", resp.statusCode());
            serverResp.putHeader("Content-Type", "application/json")
                    .setStatusCode(200);
            resp.bodyHandler(buf -> serverResp.end(buf.toString()));
        });

        req.exceptionHandler(err -> {
            LOGGER.error("Error processing GET request: {}", err);
            serverResp.putHeader("Content-Type", "text/plain")
                    .setStatusCode(500)
                    .end();

        });

        req.end();
    }

    /**
     * POST request to postman echo service, see: https://docs.postman-echo.com/?version=latest
     *
     * @param ctx {@link RoutingContext} context object
     */
    private void postPostmanEcho(RoutingContext ctx) {
        HttpClientOptions options = new HttpClientOptions()
                .setProtocolVersion(HttpVersion.HTTP_2)
                .setSsl(true)
                .setUseAlpn(true)
                .setTrustAll(true);
        HttpClient client = vertx.createHttpClient(options);

        HttpClientRequest req = client.post(443, "postman-echo.com", "/post")
                .setTimeout(2000);
        MultiMap headers = req.headers();
        headers.set("Accept", "application/json");
        headers.set("Content-Type", "application/json");

        HttpServerResponse serverResp = ctx.response();

        req.handler(resp -> {
            LOGGER.info("Response from postman echo: {}", resp.statusCode());
            serverResp.putHeader("Content-Type", "application/json")
                    .setStatusCode(200);
            resp.bodyHandler(buf -> serverResp.end(buf.toString()));
        });

        req.exceptionHandler(err -> {
            LOGGER.error("Error: processing POST request: {}", err);
            serverResp.putHeader("Content-Type", "text/plain")
                    .setStatusCode(500)
                    .end();
        });

        req.end();
    }

}
