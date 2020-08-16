package vertxwebperformance;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Server {

    public static int PORT = 8000;

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        HttpServer httpServer = vertx.createHttpServer();
        httpServer.requestHandler(r -> {
            String id = r.getParam("id");
            log.debug("id:{}", id);
            Util.timeConsumer();
            r.response().end();
        });
        httpServer.listen(PORT, r -> {
            if (r.succeeded()) {
                log.debug("server started at {}", PORT);
            } else {
                log.debug("server start error:", r.cause());
            }
        });
    }
}
