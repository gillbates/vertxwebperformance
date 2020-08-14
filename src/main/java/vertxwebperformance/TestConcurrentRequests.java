package vertxwebperformance;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestConcurrentRequests {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        for (int i = 0; i < 10; i++) {
            long start = System.currentTimeMillis();
            int finalI = i;
            HttpClient httpClient = vertx.createHttpClient();
            Future<HttpClientResponse> f = Future.future();
            HttpClientRequest abs = httpClient.getAbs("http://127.0.0.1:" + Server.PORT + "/api?id=" + i, f::complete);
            abs.exceptionHandler(f::fail);
            abs.end();
            log.debug("[{}] request sent ... ", finalI);
            f.setHandler(res -> {
                if (res.succeeded()) {
                    log.debug("[{}] used time {}", finalI, System.currentTimeMillis() - start);
                } else {
                    log.debug("[{}] error", finalI, res.cause());
                }
            });
        }
    }
}
