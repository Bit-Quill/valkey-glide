package glide.api.models;

import glide.api.models.configuration.RequestRoutingConfiguration.Route;
import java.util.Optional;
import lombok.AllArgsConstructor;

/**
 * Base class encompassing shared commands for both standalone and cluster mode implementations in a
 * transaction. Transactions allow the execution of a group of commands in a single step.
 *
 * <p>Command Response: An array of command responses is returned by the client exec command, in the
 * order they were given. Each element in the array represents a command given to the transaction.
 * The response for each command depends on the executed Redis command. Specific response types are
 * documented alongside each method.
 *
 * @example transaction = new Transaction.Builder() .set("key", "value"); .get("key"); .build();
 *     Object[] result = client.exec(transaction).get(); assertEqual(new Object[] {OK , "value"});
 */
@AllArgsConstructor
public class ClusterTransaction extends BaseTransaction {
    /** Request routing configuration */
    final Optional<Route> route;
}
