package glide.api.models;

import lombok.AllArgsConstructor;

/**
 * Extends BaseTransaction class for cluster mode commands. Transactions allow the execution of a
 * group of commands in a single step.
 *
 * <p>Command Response: An array of command responses is returned by the client exec command, in the
 * order they were given. Each element in the array represents a command given to the transaction.
 * The response for each command depends on the executed Redis command. Specific response types are
 * documented alongside each method.
 *
 * @example
 *     <pre>
 *  ClusterTransaction transaction = new ClusterTransaction();
 *  transaction.set("key", "value");
 *  transaction.get("key");
 *  ClusterValue<Object[]> result = client.exec(transaction, route).get();
 *  // result contains: OK and "value"
 *  </pre>
 */
@AllArgsConstructor
public class ClusterTransaction extends BaseTransaction {}
