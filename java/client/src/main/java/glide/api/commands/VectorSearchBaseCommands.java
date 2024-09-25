/** Copyright Valkey GLIDE Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.commands;

import glide.api.models.commands.vss.FTCreateOptions.FieldInfo;
import glide.api.models.commands.vss.FTCreateOptions.IndexType;
import java.util.concurrent.CompletableFuture;

public interface VectorSearchBaseCommands {
    // TODO GlideString???
    /**
     * Creates an index and initiates a backfill of that index.
     *
     * @see TODO
     * @param indexName Key name where index is stored.
     * @param indexType The index type.
     * @param prefixes (Optional) A list of prefixes of index definitions
     * @param fields Fields to populate into the index.
     * @return <code>OK</code>.
     * @example
     *     <pre>{@code
     * // TODO
     * }</pre>
     */
    CompletableFuture<String> ftcreate(
            String indexName, IndexType indexType, String[] prefixes, FieldInfo[] fields);
}
