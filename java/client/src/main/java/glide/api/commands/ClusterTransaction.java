package glide.api.commands;

import glide.api.models.commands.InfoOptions;
import glide.api.models.commands.SetOptions;
import glide.managers.models.Command;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Singular;
import lombok.experimental.SuperBuilder;

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
@SuperBuilder
@Getter
@EqualsAndHashCode
public class ClusterTransaction extends BaseTransaction {}
