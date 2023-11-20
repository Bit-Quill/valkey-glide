package javababushka;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

public class Test {
    public static void main(String[] args) {
        test_destructor();
        /*
        try (var client = new Client()) {
            client.connectToRedis("localhost", 6379, false, false);
            var futures = IntStream.range(0, Runtime.getRuntime().availableProcessors() * 4)
                .mapToObj(cpu -> CompletableFuture.runAsync(() -> {
                    System.out.printf("Task %d%n", cpu);
                    for (int i = 0; i < 10000; i++) {
                        client.get("dfhjkaslkgh");
                    }
                    System.out.printf("Task %d%n", cpu);
                })).toArray(CompletableFuture[]::new);
            try {
                CompletableFuture.allOf(futures).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }*/
    }

    private static void test_destructor() {
        var client1 = new Client();
        var client2 = new Client();
        /*
        client1.connectToRedis("localhost", 6379, false, false);
        System.out.println("client 1 connected");
        client1.set("ikik", "233");
        System.out.println("after client 1 set");
        client2.connectToRedis("localhost", 6379, false, false);
        System.out.println("client 2 connected");
        System.out.printf("client 2 get %s%n", client2.get("ikik"));
*/
        client1 = null;
        client2 = null;
        System.out.println("end");
        //System.exit(0);
        System.gc();
        System.out.println("end end");
    }
}
