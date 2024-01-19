package glide.api.commands;

import glide.api.models.commands.SetOptions;
import java.util.concurrent.CompletableFuture;

/** String Commands interface to handle single commands that return Strings. */
public interface StringCommands {

    CompletableFuture<String> get(String key);

    CompletableFuture<Void> set(String key, String value);

    CompletableFuture<String> set(String key, String value, SetOptions options);
}
