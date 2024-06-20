/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.configuration;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import lombok.Getter;

//  TODO doc
@Getter
public abstract class BaseSubscriptionConfiguration<T extends BaseSubscriptionConfiguration<T>> {

    //  TODO doc
    interface ChannelMode {}

    /**
     * Optional callback to accept the incoming messages. If not set, messages will be available via
     * TODO method.<br>
     * Callback arguments are:
     *
     * <ol>
     *   <li>A received message
     *   <li>User-defined context
     * </ol>
     */
    // TODO change message type
    protected Optional<BiConsumer<Object, Object>> callback = Optional.empty();

    /**
     * Optional arbitrary context, which will be passed to callback along with all received messages.
     * <br>
     * Could be used to distinguish clients if multiple clients use the shared callback.
     */
    protected Optional<Object> context = Optional.empty();

    protected <M extends ChannelMode> void addSubscription(
            Map<M, Set<String>> subscriptions, M mode, String channelOrPattern) {
        if (!subscriptions.containsKey(mode)) {
            subscriptions.put(mode, new HashSet<>());
        }
        subscriptions.get(mode).add(channelOrPattern);
    }

    /**
     * Set a callback and a context.
     *
     * @param callback The {@link #callback}.
     * @param context The {@link #context}.
     */
    @SuppressWarnings("unchecked")
    public T setCallback(BiConsumer<Object, Object> callback, Object context) {
        this.callback = Optional.ofNullable(callback);
        this.context = Optional.ofNullable(context);
        return (T) this;
    }

    /**
     * Set a callback without context. <code>null</code> will be supplied to all callback calls as a
     * context.
     *
     * @param callback The {@link #callback}.
     */
    @SuppressWarnings("unchecked")
    public T setCallback(BiConsumer<Object, Object> callback) {
        this.callback = Optional.ofNullable(callback);
        return (T) this;
    }
}
