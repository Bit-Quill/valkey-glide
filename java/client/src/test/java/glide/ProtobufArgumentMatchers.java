package glide;

import static redis_request.RedisRequestOuterClass.SimpleRoutes.AllNodes;
import static redis_request.RedisRequestOuterClass.SimpleRoutes.AllPrimaries;
import static redis_request.RedisRequestOuterClass.SimpleRoutes.Random;

import glide.api.models.configuration.RequestRoutingConfiguration;
import java.util.List;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.mockito.ArgumentMatcher;
import redis_request.RedisRequestOuterClass;

public class ProtobufArgumentMatchers {

    @AllArgsConstructor
    public static class ProtobufRouteMatcher
            implements ArgumentMatcher<RedisRequestOuterClass.RedisRequest.Builder> {
        private final RequestRoutingConfiguration.Route route;

        @Override
        public boolean matches(RedisRequestOuterClass.RedisRequest.Builder right) {
            if (route == null) {
                if (right.hasRoute()) {
                    return false;
                }
            } else {

                if (route instanceof RequestRoutingConfiguration.SimpleRoute) {
                    RedisRequestOuterClass.SimpleRoutes rightSimpleRoute = right.getRoute().getSimpleRoutes();
                    switch ((RequestRoutingConfiguration.SimpleRoute) route) {
                        case ALL_NODES:
                            if (rightSimpleRoute != AllNodes) return false;
                            break;
                        case ALL_PRIMARIES:
                            if (rightSimpleRoute != AllPrimaries) return false;
                            break;
                        case RANDOM:
                            if (rightSimpleRoute != Random) return false;
                            break;
                    }
                }

                if (route instanceof RequestRoutingConfiguration.SlotIdRoute) {
                    RedisRequestOuterClass.SlotIdRoute rightSlotIdRoute = right.getRoute().getSlotIdRoute();
                    switch (((RequestRoutingConfiguration.SlotIdRoute) route).getSlotType()) {
                        case PRIMARY:
                            if (rightSlotIdRoute.getSlotType() != RedisRequestOuterClass.SlotTypes.Primary) {
                                return false;
                            }
                            break;
                        case REPLICA:
                            if (rightSlotIdRoute.getSlotType() != RedisRequestOuterClass.SlotTypes.Replica) {
                                return false;
                            }
                            break;
                    }
                    if (((RequestRoutingConfiguration.SlotIdRoute) route).getSlotId()
                            != rightSlotIdRoute.getSlotId()) {
                        return false;
                    }
                }

                if (route instanceof RequestRoutingConfiguration.SlotKeyRoute) {
                    RedisRequestOuterClass.SlotKeyRoute rightSlotKeyRoute =
                            right.getRoute().getSlotKeyRoute();
                    switch (((RequestRoutingConfiguration.SlotKeyRoute) route).getSlotType()) {
                        case PRIMARY:
                            if (rightSlotKeyRoute.getSlotType() != RedisRequestOuterClass.SlotTypes.Primary) {
                                return false;
                            }
                            break;
                        case REPLICA:
                            if (rightSlotKeyRoute.getSlotType() != RedisRequestOuterClass.SlotTypes.Replica) {
                                return false;
                            }
                            break;
                    }
                    if (((RequestRoutingConfiguration.SlotKeyRoute) route).getSlotKey()
                            != rightSlotKeyRoute.getSlotKey()) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    @AllArgsConstructor
    public static class ProtobufSingleCommandMatcher
            implements ArgumentMatcher<RedisRequestOuterClass.RedisRequest.Builder> {
        private final RedisRequestOuterClass.RequestType requestType;
        String[] arguments;

        @Override
        public boolean matches(RedisRequestOuterClass.RedisRequest.Builder right) {
            if (right.hasSingleCommand()) {
                if (right.getSingleCommand().getRequestType() != requestType) {
                    return false;
                }

                RedisRequestOuterClass.Command.ArgsArray.Builder args =
                        RedisRequestOuterClass.Command.ArgsArray.newBuilder();
                for (String argument : arguments) {
                    args.addArgs(argument);
                }

                // compare args
                return args.build().equals(right.getSingleCommand().getArgsArray());
            }
            return false;
        }
    }

    @AllArgsConstructor
    public static class ProtobufTransactionMatcher
            implements ArgumentMatcher<RedisRequestOuterClass.RedisRequest.Builder> {
        private final List<Pair<RedisRequestOuterClass.RequestType, String[]>> left;

        @Override
        public boolean matches(RedisRequestOuterClass.RedisRequest.Builder right) {
            if (right.hasTransaction()) {

                RedisRequestOuterClass.Transaction transaction = right.getTransaction();
                for (int idx = 0; idx < transaction.getCommandsCount(); idx++) {
                    RedisRequestOuterClass.Command command = transaction.getCommands(idx);
                    if (command.getRequestType() != left.get(idx).getLeft()) {
                        return false;
                    }

                    RedisRequestOuterClass.Command.ArgsArray.Builder args =
                            RedisRequestOuterClass.Command.ArgsArray.newBuilder();
                    for (String argument : left.get(idx).getRight()) {
                        args.addArgs(argument);
                    }

                    // compare args
                    if (!args.build().equals(command.getArgsArray())) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
    }
}
