package vertxwebperformance;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class TestConcurrentRequests {
    private static long start = System.currentTimeMillis();
    static long firstResponseTime = 0;

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        HttpClient httpClient = vertx.createHttpClient();

        int count = 10;
        List<Future> list = new ArrayList();

        HttpClientRequest[] abs = new HttpClientRequest[count];
        for (int i = 0; i < count; i++) {
            Future<HttpClientResponse> future = Future.future();
            list.add(future);
            int finalI = i;
            long thisStart = System.currentTimeMillis();
            abs[i] = httpClient.getAbs("http://127.0.0.1:" + Server.PORT + "/api?id=" + i, response -> {
                if (response.statusCode() == 200) {
                    log.debug("[{}] used time {}ms, total {} ms ... ", finalI, System.currentTimeMillis() - thisStart, System.currentTimeMillis() - start);
                } else {
                    log.debug("[{}] error", finalI);
                }
                future.complete(response);
            });
        }

        start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            abs[i].end();
        }

        CompositeFuture.all(list).setHandler(ar -> {
            if (ar.succeeded()) {
                log.debug("all success");
                log.debug("total process time {}", System.currentTimeMillis() - start);
            } else {
                log.debug("some request failed");
            }
        });
    }
}
