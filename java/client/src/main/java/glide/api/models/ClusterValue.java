package glide.api.models;

import java.util.Map;

/**
 * A union-like type which can store single or bulk value retrieved from Redis.
 *
 * @param <T> The wrapped data type
 */
public class ClusterValue<T> {
    private Map<String, T> multiValue = null;

    private T singleValue = null;

    private ClusterValue() {}

    /** Get per-node value. */
    public Map<String, T> getMultiValue() {
        assert hasMultiData();
        return multiValue;
    }

    /** Get the single value. */
    public T getSingleValue() {
        assert !hasMultiData();
        return singleValue;
    }

    @SuppressWarnings("unchecked")
    public static <T> ClusterValue<T> of(Object data) {
        var res = new ClusterValue<T>();
        if (data instanceof Map) {
            res.multiValue = (Map<String, T>) data;
        } else {
            res.singleValue = (T) data;
        }
        return res;
    }

    /** Get the value type. Use it prior to accessing the data. */
    public boolean hasMultiData() {
        return multiValue != null;
    }
}
