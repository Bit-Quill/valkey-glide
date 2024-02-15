package glide.api.commands;

import glide.api.models.commands.ZaddOptions;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface SortedSetCommands {
    CompletableFuture<Long> zadd(String key, Map<String, Double> membersScoresMap, ZaddOptions options, boolean changed);
    CompletableFuture<Double> zaddIncr(String key, String member, double increment, ZaddOptions options);
    CompletableFuture<Long> zrem(String key, String[] members);
    CompletableFuture<Long> zcard(String key);
}