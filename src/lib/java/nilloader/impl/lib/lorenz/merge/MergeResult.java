/*
 * This file is part of Lorenz, licensed under the MIT License (MIT).
 *
 * Copyright (c) Jamie Mansfield <https://www.jamierocks.uk/>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package nilloader.impl.lib.lorenz.merge;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * This class represents the result of a mapping merge handled by a {@link MappingSetMergerHandler}. This
 * class is only relevant to mapping types which have children, which includes both top level and inner class mappings,
 * and method mappings.
 * </p>
 * <p>
 * This class holds 2 values:
 * </p>
 * <ol>
 *     <li>The merge result</li>
 *     <li>The child mappings on the right to merge into the merge result with the left side of the merge</li>
 * </ol>
 * <p>
 * When a merge occurs with mappings which contain child data the merger will need to recurse into the child data and
 * merge those mappings too. Depending on the situation this may not be desirable, or you may wish to control
 * specifically which mappings do or don't get merged. Typically there will only be one mapping on the right side, but
 * some situations, such as duplicates and method mappings, may have multiple mappings that you may want merged. By
 * storing those mappings in a list in that class that allows the merger to have the most flexibility in choosing which
 * mappings are or aren't merged.
 * </p>
 *
 * @apiNote This class should only contains the mappings on the <b>right side</b> of the merge which should be mapped.
 *          The left side is considered the source and is always mapped.
 *
 * @param <T> The type of the result of the merge handler.
 *
 * @author Kyle Wood
 * @since 0.5.4
 */
public class MergeResult<T> {

    private final T result;
    private final List<T> mappingsToMap;

    /**
     * Create a new {@code MergeResult} with a result and no mappings to merge.
     * @param result The result of the merge operation, may be {@code null}.
     */
    public MergeResult(final T result) {
        this.result = result;
        this.mappingsToMap = Collections.emptyList();
    }

    /**
     * Create a new {@code MergeResult} with a result and only one mapping to merge.
     * @param result The result of the merge operation, may be {@code null}.
     * @param mappingToMerge The mapping to merge into the {@code result}, must not be {@code null}.
     */
    public MergeResult(final T result, final T mappingToMerge) {
        this.result = Objects.requireNonNull(result, "Merge action result must not be null");
        this.mappingsToMap = Collections.singletonList(Objects.requireNonNull(mappingToMerge, "Merge action mapping must not be null"));
    }

    /**
     * Create a new {@code MergeResult} with a result and a list of mappings to merge.
     * @param result The result of the merge operation, may be {@code null}.
     * @param mappingsToMap The mappings to merge into the {@code result}, must not be {@code null}, and no entries
     *                        may be {@code null}. May be empty.
     */
    public MergeResult(final T result, final List<T> mappingsToMap) {
        this.result = Objects.requireNonNull(result, "Merge action result must not be null");
        this.mappingsToMap = Objects.requireNonNull(mappingsToMap, "Merge action mappings must not be null");
    }

    /**
     * @return The returned result of the duplicate merge action, may be {@code null}.
     */
    public T getResult() {
        return this.result;
    }

    /**
     * @return The mappings to map child entries from into the new mapping returned by {@link #getResult()}. Cannot be
     *         {@code null}.
     */
    public List<T> getMappingsToMap() {
        return this.mappingsToMap;
    }

    @Override
    public String toString() {
        return "MergeResult{" +
            "result=" + this.result +
            ", mappingsToMap=" + this.mappingsToMap +
            '}';
    }
}
