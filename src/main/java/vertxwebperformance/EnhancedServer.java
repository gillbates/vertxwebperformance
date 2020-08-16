package vertxwebperformance;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EnhancedServer {

    public static int PORT = 8000;

    public static void main(String[] args) {
        int cores = Runtime.getRuntime().availableProcessors();
        log.debug("{} cores found ...", cores);
        VertxOptions options = new VertxOptions();
//        options.setEventLoopPoolSize(cores * 2);
        Vertx vertx = Vertx.vertx(options); // (1)

        DeploymentOptions depOps = new DeploymentOptions();
        depOps.setInstances(cores);
        vertx.deployVerticle(HttpVerticle.class, depOps, ar -> { // (2)
            if (ar.succeeded()) {
                log.debug("done deployment");

            } else {
                log.error("{}", ar.cause());
            }
        });
    }
}
