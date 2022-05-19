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

import java.util.Objects;

import nilloader.impl.lib.lorenz.MappingSet;

/**
 * <p>
 * This class represents the current context for a remapping operation for a {@link MappingSetMergerHandler}. This
 * allows implementers of {@link MappingSetMergerHandler}s access to each of the 4 combinations of mappings for a merge:
 * </p>
 * <ul>
 *     <li>The left mapping set</li>
 *     <li>The right mapping set</li>
 *     <li>The reverse of the left mapping set</li>
 *     <li>The reverse of the right mapping set</li>
 * </ul>
 * <p>
 * This class represents a performance advantage for retrieving the reverse of the mappings as well, as it only computes
 * the value of the reverse mapping set when requested, and only computes the value once per merge session.
 * </p>
 * <p>
 * This class is thread safe and all methods are reentrant.
 * </p>
 *
 * @author Kyle Wood
 * @since 0.5.4
 */
@SuppressWarnings("unused") // Unused methods for third-party handler implementations
public class MergeContext {

    private final MappingSet left;
    private final MappingSet right;

    private MappingSet leftReversed;
    private MappingSet rightReversed;

    /**
     * Create a new {@link MappingSet} merge context. Provide the left and right mapping sets for the merge, neither may
     * be {@code null}.
     *
     * @param left The {@code left} mapping set, must not be {@code null}.
     * @param right The {@code right} mapping set, must not be {@code null}.
     */
    public MergeContext(final MappingSet left, final MappingSet right) {
        this.left = Objects.requireNonNull(left, "Left MappingSet may not be null");
        this.right = Objects.requireNonNull(right, "Right MappingSet may not be null");
    }

    /**
     * @return The {@code left} {@link MappingSet} for the merge operation, not {@code null}.
     */
    public MappingSet getLeft() {
        return this.left;
    }

    /**
     * @return The {@code right} {@link MappingSet} for the merge operation, not {@code null}.
     */
    public MappingSet getRight() {
        return this.right;
    }

    /**
     * @return The {@link #getRight() right} {@link MappingSet} for the merge operation, but reversed. Never
     *         {@code null}. This method computes the value on first query and caches the result, making it more
     *         efficient than calling {@link MappingSet#reverse()} yourself.
     */
    public MappingSet getLeftReversed() {
        MappingSet ctx = this.leftReversed;
        if (ctx != null) {
            return ctx;
        }

        synchronized (this.left) {
            ctx = this.leftReversed;
            if (ctx != null) {
                return ctx;
            }
            this.leftReversed = this.left.reverse();
            ctx = this.leftReversed;
        }

        return ctx;
    }

    /**
     * @return The {@link #getRight() right} {@link MappingSet} for the merge operation, but reversed. Never
     *         {@code null}. This method computes the value on first query and caches the result, making it more
     *         efficient than calling {@link MappingSet#reverse()} yourself.
     */
    public MappingSet getRightReversed() {
        MappingSet ctx = this.rightReversed;
        if (ctx != null) {
            return ctx;
        }

        synchronized (this.right) {
            ctx = this.rightReversed;
            if (ctx != null) {
                return ctx;
            }
            this.rightReversed = this.right.reverse();
            ctx = this.rightReversed;
        }

        return ctx;
    }
}
