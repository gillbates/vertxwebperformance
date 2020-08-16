package vertxwebperformance;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.*;


@Slf4j
public class Util {
    public static void timeConsumer() {
        long start = System.currentTimeMillis();

        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long total = System.currentTimeMillis() - start;

        log.debug("timeConsumer used {} ms ...", total);
    }

    public static void main(String[] args) {
        timeConsumer();
    }
}
