package vertxwebperformance;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestSequentialRequests {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        Future<Void> future = Future.succeededFuture();

        long firstStart = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            int finalI = i;
            HttpClient httpClient = vertx.createHttpClient();
            Future<HttpClientResponse> f = Future.future();
            HttpClientRequest abs = httpClient.getAbs("http://127.0.0.1:" + Server.PORT + "/api?id=" + i, f::complete);
            abs.exceptionHandler(f::fail);
            future = future.compose(v -> {
                Future<Void> ff = Future.future();
                long start = System.currentTimeMillis();
                abs.end();
                log.debug("[{}] request sent ... ", finalI);
                f.setHandler(res -> {
                    if (res.succeeded()) {
                        ff.complete();
                        long total = System.currentTimeMillis() - firstStart;
                        log.debug("[{}] used time {}ms, total {} ms ... ", finalI, System.currentTimeMillis() - start, System.currentTimeMillis() - firstStart);


                    } else {

                        ff.fail(res.cause());
                        log.debug("[{}] error ", finalI, res.cause());
                    }
                });
                return ff;
            });
        }
    }
}
