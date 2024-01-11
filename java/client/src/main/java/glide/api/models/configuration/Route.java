package glide.api.models.configuration;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Request routing configuration. */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Route {

    public enum RouteType {
        /** Route request to all nodes. */
        ALL_NODES,
        /** Route request to all primary nodes. */
        ALL_PRIMARIES,
        /** Route request to a random node. */
        RANDOM,
        /** Route request to the primary node that contains the slot with the given id. */
        PRIMARY_SLOT_ID,
        /** Route request to the replica node that contains the slot with the given id. */
        REPLICA_SLOT_ID,
        /** Route request to the primary node that contains the slot that the given key matches. */
        PRIMARY_SLOT_KEY,
        /** Route request to the replica node that contains the slot that the given key matches. */
        REPLICA_SLOT_KEY,
    }

    /**
     * Request routing configuration overrides the {@link ReadFrom} connection configuration.<br>
     * If {@link RouteType#REPLICA_SLOT_ID} or {@link RouteType#REPLICA_SLOT_KEY} is used, the request
     * will be routed to a replica, even if the strategy is {@link ReadFrom#PRIMARY}.
     */
    private final RouteType routeType;

    /**
     * Slot number. There are 16384 slots in a redis cluster, and each shard manages a slot range.
     * Unless the slot is known, it's better to route using {@link RouteType#PRIMARY_SLOT_KEY} or
     * {@link RouteType#REPLICA_SLOT_KEY}.<br>
     * Could be used with {@link RouteType#PRIMARY_SLOT_ID} or {@link RouteType#REPLICA_SLOT_ID} only.
     */
    private final int slotId;

    /**
     * The request will be sent to nodes managing this key.<br>
     * Could be used with {@link RouteType#PRIMARY_SLOT_KEY} or {@link RouteType#REPLICA_SLOT_KEY}
     * only.
     */
    private final String slotKey;

    @RequiredArgsConstructor
    public static class Builder {
        private final RouteType routeType;
        private int slotId;
        private boolean slotIdSet = false;
        private String slotKey;
        private boolean slotKeySet = false;

        public Builder setSlotId(int slotId) {
            if (!(routeType == RouteType.PRIMARY_SLOT_ID || routeType == RouteType.REPLICA_SLOT_ID)) {
                throw new IllegalArgumentException(
                        "Slot ID could be set for corresponding types of route only");
            }
            this.slotId = slotId;
            slotIdSet = true;
            return this;
        }

        public Builder setSlotKey(String slotKey) {
            if (!(routeType == RouteType.PRIMARY_SLOT_KEY || routeType == RouteType.REPLICA_SLOT_KEY)) {
                throw new IllegalArgumentException(
                        "Slot key could be set for corresponding types of route only");
            }
            this.slotKey = slotKey;
            slotKeySet = true;
            return this;
        }

        public Route build() {
            if ((routeType == RouteType.PRIMARY_SLOT_ID || routeType == RouteType.REPLICA_SLOT_ID)
                    && !slotIdSet) {
                throw new IllegalArgumentException("Slot ID is missing");
            }
            if ((routeType == RouteType.PRIMARY_SLOT_KEY || routeType == RouteType.REPLICA_SLOT_KEY)
                    && !slotKeySet) {
                throw new IllegalArgumentException("Slot key is missing");
            }

            return new Route(routeType, slotId, slotKey);
        }
    }
}
