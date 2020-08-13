# vertx web performance issues

## 1 request handler called not stable

### Version

vertx 3.6, 3.9+ (earlier versions not tested ...)
Java 8,11,13

### Context

We have used vertx webserver as our web in production. But it seemed that its eventloop cannot call its handler at a stable rate even for the simplest handle function.
In our production envinronment, several non-blocking requests are delayed but without any pressure on CPU.

Therefore, we have implemented following code to reproduce the issue.

### Reproducer

1, start the webserver using simplest code.

```
        Vertx vertx = Vertx.vertx();
        HttpServer httpServer = vertx.createHttpServer();
        httpServer.requestHandler(r -> {
            String id = r.getParam("id");
            log.debug("id:{}", id);
            r.response().end();
        });
        httpServer.listen(PORT, r -> {
            if (r.succeeded()) {
                log.debug("server started at {}", PORT);
            } else {
                log.debug("server start error:", r.cause());
            }
        });
```
* https://github.com/gillbates/vertxwebperformance/blob/master/src/main/java/vertxwebperformance/Server.java

2, start the client for 10 sequential requests to localhost:8000 and check the timestamp for request received at server console.

```
        Vertx vertx = Vertx.vertx();
        Future<Void> future = Future.succeededFuture();
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
                        log.debug("[{}] used time {}} ", finalI, System.currentTimeMillis() - start);
                    } else {
                        ff.fail(res.cause());
                        log.debug("[{}] error ", finalI, res.cause());
                    }
                });
                return ff;
            });
        }
```
* https://github.com/gillbates/vertxwebperformance/blob/master/src/main/java/vertxwebperformance/TestSequentialRequests.java

3, you will get following log in console

server log

```
[17:19:47.998][vert.x-eventloop-thread-1][DEBUG][v.Server][22] - [server started at 8000]
[17:19:53.322][vert.x-eventloop-thread-1][DEBUG][v.Server][17] - [id:0]
[17:19:53.336][vert.x-eventloop-thread-1][DEBUG][v.Server][17] - [id:1]
[17:19:53.374][vert.x-eventloop-thread-1][DEBUG][v.Server][17] - [id:2]
[17:19:53.378][vert.x-eventloop-thread-1][DEBUG][v.Server][17] - [id:3]
[17:19:53.382][vert.x-eventloop-thread-1][DEBUG][v.Server][17] - [id:4]
[17:19:53.385][vert.x-eventloop-thread-1][DEBUG][v.Server][17] - [id:5]
[17:19:53.424][vert.x-eventloop-thread-1][DEBUG][v.Server][17] - [id:6]
[17:19:53.428][vert.x-eventloop-thread-1][DEBUG][v.Server][17] - [id:7]
[17:19:53.431][vert.x-eventloop-thread-1][DEBUG][v.Server][17] - [id:8]
[17:19:53.434][vert.x-eventloop-thread-1][DEBUG][v.Server][17] - [id:9]
```

client log

```
[17:19:53.081][main][DEBUG][v.TestSequentialRequests][25] - [[0] request sent ... ]
[17:19:53.333][vert.x-eventloop-thread-2][DEBUG][v.TestSequentialRequests][25] - [[1] request sent ... ]
[17:19:53.333][vert.x-eventloop-thread-2][DEBUG][v.TestSequentialRequests][29] - [[0] used time 262} ]
[17:19:53.337][vert.x-eventloop-thread-2][DEBUG][v.TestSequentialRequests][25] - [[2] request sent ... ]
[17:19:53.337][vert.x-eventloop-thread-2][DEBUG][v.TestSequentialRequests][29] - [[1] used time 4} ]
[17:19:53.375][vert.x-eventloop-thread-2][DEBUG][v.TestSequentialRequests][25] - [[3] request sent ... ]
[17:19:53.376][vert.x-eventloop-thread-2][DEBUG][v.TestSequentialRequests][29] - [[2] used time 39} ]
[17:19:53.379][vert.x-eventloop-thread-2][DEBUG][v.TestSequentialRequests][25] - [[4] request sent ... ]
[17:19:53.379][vert.x-eventloop-thread-2][DEBUG][v.TestSequentialRequests][29] - [[3] used time 4} ]
[17:19:53.383][vert.x-eventloop-thread-2][DEBUG][v.TestSequentialRequests][25] - [[5] request sent ... ]
[17:19:53.383][vert.x-eventloop-thread-2][DEBUG][v.TestSequentialRequests][29] - [[4] used time 5} ]
[17:19:53.386][vert.x-eventloop-thread-2][DEBUG][v.TestSequentialRequests][25] - [[6] request sent ... ]
[17:19:53.386][vert.x-eventloop-thread-2][DEBUG][v.TestSequentialRequests][29] - [[5] used time 3} ]
[17:19:53.425][vert.x-eventloop-thread-2][DEBUG][v.TestSequentialRequests][25] - [[7] request sent ... ]
[17:19:53.426][vert.x-eventloop-thread-2][DEBUG][v.TestSequentialRequests][29] - [[6] used time 40} ]
[17:19:53.429][vert.x-eventloop-thread-2][DEBUG][v.TestSequentialRequests][25] - [[8] request sent ... ]
[17:19:53.429][vert.x-eventloop-thread-2][DEBUG][v.TestSequentialRequests][29] - [[7] used time 4} ]
[17:19:53.432][vert.x-eventloop-thread-2][DEBUG][v.TestSequentialRequests][25] - [[9] request sent ... ]
[17:19:53.432][vert.x-eventloop-thread-2][DEBUG][v.TestSequentialRequests][29] - [[8] used time 3} ]
[17:19:53.435][vert.x-eventloop-thread-2][DEBUG][v.TestSequentialRequests][29] - [[9] used time 3} ]
```
4, From client log, we found that the requests have different time cost, especially request 6.
From server log, we found that:

* request 6 was sent on 17:19:53.386 from client. 
* But its handler was called by eventloop on 17:19:53.424

So I doubt why is this request delayed? Is it because its single thread of the eventloop for this WebServer?

## 2 increased response time for concurrent requests

### Context

We've found that the request will be much more slower when vertx receive concurrent requests.

Therefore, we have implemented following code to reproduce the issue.

### Reproducer
 
1, start the webserver using simplest code.
 
 ```
         Vertx vertx = Vertx.vertx();
         HttpServer httpServer = vertx.createHttpServer();
         httpServer.requestHandler(r -> {
             String id = r.getParam("id");
             log.debug("id:{}", id);
             r.response().end();
         });
         httpServer.listen(PORT, r -> {
             if (r.succeeded()) {
                 log.debug("server started at {}", PORT);
             } else {
                 log.debug("server start error:", r.cause());
             }
         });
 ```
 * https://github.com/gillbates/vertxwebperformance/blob/master/src/main/java/vertxwebperformance/Server.java
 
2, start the client for 10 concurrent requests to localhost:8000 and check the timestamp for request received at server console.

```
        Vertx vertx = Vertx.vertx();
        Future<Void> future = Future.succeededFuture();
        for (int i = 0; i < 10; i++) {
                 int finalI = i;
                 HttpClient httpClient = vertx.createHttpClient();
                 Future<HttpClientResponse> f = Future.future();
                 HttpClientRequest abs = httpClient.getAbs("http://127.0.0.1:" + Server.PORT + "/api?id=" + i, f::complete);
                 abs.exceptionHandler(f::fail);
                 abs.end();
                 log.debug("[{}] request sent ... ", finalI);
                 long start = System.currentTimeMillis();
                 f.setHandler(res -> {
                 if (res.succeeded()) {
                   log.debug("[{}] used time {}", finalI, System.currentTimeMillis() - start);
                 } else {
                 log.debug("[{}] error", finalI, res.cause());
               }
            });
        }
```
* https://github.com/gillbates/vertxwebperformance/blob/master/src/main/java/vertxwebperformance/TestConcurrentRequests.java

3, you will get following log in console

server log

```
[18:16:17.631][vert.x-eventloop-thread-1][DEBUG][v.Server][22] - [server started at 8000]
[18:16:28.796][vert.x-eventloop-thread-1][DEBUG][v.Server][17] - [id:5]
[18:16:28.801][vert.x-eventloop-thread-1][DEBUG][v.Server][17] - [id:6]
[18:16:28.802][vert.x-eventloop-thread-1][DEBUG][v.Server][17] - [id:1]
[18:16:28.803][vert.x-eventloop-thread-1][DEBUG][v.Server][17] - [id:0]
[18:16:28.803][vert.x-eventloop-thread-1][DEBUG][v.Server][17] - [id:2]
[18:16:28.804][vert.x-eventloop-thread-1][DEBUG][v.Server][17] - [id:7]
[18:16:28.805][vert.x-eventloop-thread-1][DEBUG][v.Server][17] - [id:3]
[18:16:28.806][vert.x-eventloop-thread-1][DEBUG][v.Server][17] - [id:4]
[18:16:28.808][vert.x-eventloop-thread-1][DEBUG][v.Server][17] - [id:9]
[18:16:28.809][vert.x-eventloop-thread-1][DEBUG][v.Server][17] - [id:8]

```

client log

```
[18:16:28.493][main][DEBUG][v.TestConcurrentRequests][22] - [[0] request sent ... ]
[18:16:28.498][main][DEBUG][v.TestConcurrentRequests][22] - [[1] request sent ... ]
[18:16:28.499][main][DEBUG][v.TestConcurrentRequests][22] - [[2] request sent ... ]
[18:16:28.500][main][DEBUG][v.TestConcurrentRequests][22] - [[3] request sent ... ]
[18:16:28.500][main][DEBUG][v.TestConcurrentRequests][22] - [[4] request sent ... ]
[18:16:28.501][main][DEBUG][v.TestConcurrentRequests][22] - [[5] request sent ... ]
[18:16:28.502][main][DEBUG][v.TestConcurrentRequests][22] - [[6] request sent ... ]
[18:16:28.502][main][DEBUG][v.TestConcurrentRequests][22] - [[7] request sent ... ]
[18:16:28.503][main][DEBUG][v.TestConcurrentRequests][22] - [[8] request sent ... ]
[18:16:28.503][main][DEBUG][v.TestConcurrentRequests][22] - [[9] request sent ... ]
[18:16:28.810][vert.x-eventloop-thread-2][DEBUG][v.TestConcurrentRequests][26] - [[0] used time 313]
[18:16:28.810][vert.x-eventloop-thread-23][DEBUG][v.TestConcurrentRequests][26] - [[7] used time 308]
[18:16:28.810][vert.x-eventloop-thread-5][DEBUG][v.TestConcurrentRequests][26] - [[1] used time 311]
[18:16:28.810][vert.x-eventloop-thread-17][DEBUG][v.TestConcurrentRequests][26] - [[5] used time 308]
[18:16:28.810][vert.x-eventloop-thread-20][DEBUG][v.TestConcurrentRequests][26] - [[6] used time 308]
[18:16:28.810][vert.x-eventloop-thread-8][DEBUG][v.TestConcurrentRequests][26] - [[2] used time 310]
[18:16:28.810][vert.x-eventloop-thread-11][DEBUG][v.TestConcurrentRequests][26] - [[3] used time 310]
[18:16:28.810][vert.x-eventloop-thread-14][DEBUG][v.TestConcurrentRequests][26] - [[4] used time 309]
[18:16:28.812][vert.x-eventloop-thread-5][DEBUG][v.TestConcurrentRequests][26] - [[9] used time 309]
[18:16:28.812][vert.x-eventloop-thread-2][DEBUG][v.TestConcurrentRequests][26] - [[8] used time 309]

```

4, when we compare this result to sequential requests in first issue. We found that its strange that response time rises from 3s to 300s on average while server handler does nothing special.

*So I doubt why is this request time increase? Is it because its single thread of the eventloop for this WebServer?