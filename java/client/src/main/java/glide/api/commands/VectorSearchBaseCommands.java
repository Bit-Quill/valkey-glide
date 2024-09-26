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
     * // Create an index for vectors of size 2:
     * client.ftcreate("hash_idx1", IndexType.HASH, new String[] {"hash:"}, new FieldInfo[] {
     *     new FieldInfo("vec", "VEC", VectorFieldFlat.builder(DistanceMetric.L2, 2).build())
     * }).get();
     * // Create a 6-dimensional JSON index using the HNSW algorithm:
     * client.ftcreate("json_idx1", IndexType.JSON, new String[] {"json:"}, new FieldInfo[] {
     *     new FieldInfo("$.vec", "VEC", VectorFieldHnsw.builder(DistanceMetric.L2, 6).numberOfEdges(32).build())
     * }).get();
     * }</pre>
     */
    CompletableFuture<String> ftcreate(
            String indexName, IndexType indexType, String[] prefixes, FieldInfo[] fields);
}
