package io.woshiadai.starter;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.impl.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.util.Date;

/**
 * Code based on {@link io.vertx.ext.web.handler.impl.LoggerHandlerImpl}.
 */
public class Slf4jRequestLogger implements RequestLogHandler {
  private final Logger logger;
  private final DateFormat dateTimeFormat;
  private final boolean immediate;
  private final LoggerFormat format;

  public Slf4jRequestLogger(boolean immediate, LoggerFormat format) {
    this.logger = LoggerFactory.getLogger(Slf4jRequestLogger.class);
    this.dateTimeFormat = DateFormat.getDateTimeInstance();
    this.immediate = immediate;
    this.format = format;
  }

  public Slf4jRequestLogger(LoggerFormat format) {
    this(false, format);
  }

  private String getClientAddress(SocketAddress inetSocketAddress) {
    return inetSocketAddress == null ? null : inetSocketAddress.host();
  }

  private void log(RoutingContext context, long timestamp,
      String remoteClient, HttpVersion version,
      HttpMethod method, String uri) {
    HttpServerRequest request = context.request();
    long contentLength = 0L;
    String versionFormatted;
    if (this.immediate) {
      versionFormatted = request.headers().get("content-length");
      if (versionFormatted != null) {
        try {
          contentLength = Long.parseLong(versionFormatted.toString());
        } catch (NumberFormatException var17) {
          contentLength = 0L;
        }
      }
    } else {
      contentLength = request.response().bytesWritten();
    }

    versionFormatted = "-";
    switch (version) {
      case HTTP_1_0:
        versionFormatted = "HTTP/1.0";
        break;
      case HTTP_1_1:
        versionFormatted = "HTTP/1.1";
        break;
      case HTTP_2:
        versionFormatted = "HTTP/2.0";
        break;
      default:
        versionFormatted = "-";
        break;
    }

    MultiMap headers = request.headers();
    int status = request.response().getStatusCode();
    String message = null;
    switch (this.format) {
      case DEFAULT:
        String referrer = headers.contains("referrer") ? headers.get("referrer") : headers.get("referer");
        String userAgent = headers.get("user-agent");
        String body = context.getBodyAsString().replaceAll("(\\r|\\n)", "");
        referrer = referrer == null ? "-" : referrer;
        userAgent = userAgent == null ? "-" : userAgent;
        message = String.format("%s - - [%s] \"%s %s %s\" %d %d \"%s\" \"%s\" %dms %s",
            remoteClient,
            this.dateTimeFormat.format(new Date(timestamp)),
            method,
            uri,
            versionFormatted,
            status,
            contentLength,
            referrer,
            userAgent,
            System.currentTimeMillis() - timestamp,
            body);
        break;
      case SHORT:
        message = String.format("%s - - [%s] \"%s %s %s\" %d %d \"%s\" \"%s\" %dms",
            remoteClient,
            this.dateTimeFormat.format(new Date(timestamp)),
            method,
            uri,
            versionFormatted,
            status,
            contentLength,
            headers.contains("referrer") ? headers.get("referrer") : headers.get("referer"),
            headers.get("user-agent"),
            System.currentTimeMillis() - timestamp);
        break;
      case TINY:
        message = String.format("%s - %s %s %d %d",
            remoteClient,
            method,
            uri,
            status,
            contentLength);
        break;
      default:
        message = String.format("%s - %s %s %d %d",
            remoteClient,
            method,
            uri,
            status,
            contentLength);
        break;
    }

    this.doLog(status, message);
  }

  protected void doLog(int status, String message) {
    if (status >= 500) {
      this.logger.error(message);
    } else if (status >= 400) {
      this.logger.warn(message);
    } else {
      this.logger.info(message);
    }

  }

  /**
   * Core logic for the handler.
   *
   * @param context     {@link RoutingContext} routing context
   */
  public void handle(RoutingContext context) {
    long timestamp = System.currentTimeMillis();
    String remoteClient = this.getClientAddress(context.request().remoteAddress());
    HttpMethod method = context.request().method();
    String uri = context.request().uri();
    HttpVersion version = context.request().version();
    if (this.immediate) {
      this.log(context, timestamp, remoteClient, version, method, uri);
    } else {
      context.addBodyEndHandler((handler) -> {
        this.log(context, timestamp, remoteClient, version, method, uri);
      });
    }

    context.next();
  }
}
