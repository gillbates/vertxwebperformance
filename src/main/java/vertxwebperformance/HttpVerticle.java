package vertxwebperformance;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpVerticle extends AbstractVerticle {
    @Override
    public void start(Future<Void> startFut) throws Exception {
        vertx.createHttpServer()
                .requestHandler(req -> {
                    String id = req.getParam("id");
                    log.debug("id:{}", id);
                    Util.timeConsumer();
                    req.response().end();
                })
                .listen(EnhancedServer.PORT, result -> { // (1)
                    if (!result.succeeded()) { // (2)
                        System.out.println("failed to start server, msg = " + result.cause());
                        startFut.fail(result.cause()); // (3)
                    } else {
                        log.debug("server listening on {} ...", EnhancedServer.PORT);
                    }
                });
    }
}
