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

/**
 * Configuration object for {@link MappingSetMerger}. Create new instances via {@link #builder()}.
 *
 * @author Kyle Wood
 * @since 0.5.4
 */
public final class MergeConfig {

    private final MappingSetMergerHandler handler;

    private final MethodMergeStrategy methodMergeStrategy;
    private final FieldMergeStrategy fieldMergeStrategy;

    private final int parallelism;

    MergeConfig(final MappingSetMergerHandler handler, final MethodMergeStrategy methodMergeStrategy, final FieldMergeStrategy fieldMergeStrategy, final int parallelism) {
        this.handler = Objects.requireNonNull(handler, "handler must not be null");
        this.methodMergeStrategy = Objects.requireNonNull(methodMergeStrategy, "methodMergeStrategy must not be null");
        this.fieldMergeStrategy = Objects.requireNonNull(fieldMergeStrategy, "fieldMergeStrategy must not be null");
        if (parallelism == -1 || parallelism > 0) {
            this.parallelism = parallelism;
        } else {
            throw new IllegalArgumentException("Illegal parallelism value: " + parallelism);
        }
    }

    /**
     * The merge handler for the merge session. Defaults to the default implementation when not specified.
     *
     * @return The merge handler to use for the merge operation. Never {@code null}.
     * @see MappingSetMergerHandler
     */
    public MappingSetMergerHandler getHandler() {
        return this.handler;
    }

    /**
     * The merge strategy to use for merging method mappings for the merge session. Defaults to {@link MethodMergeStrategy#STRICT} when not specified.
     *
     * @return The merge strategy to use for merging method mappings. Never {@code null}.
     */
    public MethodMergeStrategy getMethodMergeStrategy() {
        return this.methodMergeStrategy;
    }

    /**
     * The merge strategy to use for merging field mappings for the merge session. Defaults to {@link FieldMergeStrategy#LOOSE} when not specified.
     *
     * @return The merge strategy to use for merging field mappings. Never {@code null}.
     */
    public FieldMergeStrategy getFieldMergeStrategy() {
        return this.fieldMergeStrategy;
    }

    /**
     * The parallelism level to use for the {@link java.util.concurrent.Executors#newWorkStealingPool(int) work stealing pool} used for the merge
     * session. A value of {@code -1} is the default and means
     * {@link java.util.concurrent.Executors#newWorkStealingPool() Executors.newWorkStealingPool()} will be used instead to create the work stealing
     * pool.
     *
     * @return The parallelism level to use for the work stealing pool used for the merge.
     * @since 0.5.6
     */
    public int getParallelism() {
        return this.parallelism;
    }

    /**
     * Create a new {@link Builder} to create new instances of {@link MergeConfig}.
     *
     * @return A new {@link Builder} instance, never {@code null}.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "MergeConfig{" +
            "handler=" + this.handler +
            ", methodMergeStrategy=" + this.methodMergeStrategy +
            ", fieldMergeStrategy=" + this.fieldMergeStrategy +
            ", parallelism=" + this.parallelism +
            '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        final MergeConfig that = (MergeConfig) o;
        return this.parallelism == that.parallelism
            && this.handler.equals(that.handler)
            && this.methodMergeStrategy == that.methodMergeStrategy &&
            this.fieldMergeStrategy == that.fieldMergeStrategy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.handler, this.methodMergeStrategy, this.fieldMergeStrategy, this.parallelism);
    }

    /**
     * This builder class creates instances for {@link MergeConfig}.
     *
     * @see MergeConfig
     */
    public static final class Builder {

        private MappingSetMergerHandler handler = new MappingSetMergerHandler() {};

        private MethodMergeStrategy methodMergeStrategy = MethodMergeStrategy.STRICT;

        private FieldMergeStrategy fieldMergeStrategy = FieldMergeStrategy.LOOSE;

        private int parallelism = -1;

        Builder() {}

        /**
         * Set the {@link MappingSetMergerHandler handler} for the merge session. Defaults to the default implementation.
         *
         * @param handler The merge handler to use. Must not be {@code null}.
         * @return {@code this} instance for chaining.
         * @see MergeConfig#getHandler()
         */
        public Builder withMergeHandler(final MappingSetMergerHandler handler) {
            this.handler = Objects.requireNonNull(handler);
            return this;
        }

        /**
         * Set the {@link MethodMergeStrategy} for merging method mappings for the merge session. Defaults to {@link MethodMergeStrategy#STRICT}.
         *
         * @param methodMergeStrategy The method merge strategy to use. Must not be {@code null}.
         * @return {@code this} instance for chaining.
         * @see MergeConfig#getMethodMergeStrategy()
         */
        public Builder withMethodMergeStrategy(final MethodMergeStrategy methodMergeStrategy) {
            this.methodMergeStrategy = Objects.requireNonNull(methodMergeStrategy);
            return this;
        }

        /**
         * Set the {@link FieldMergeStrategy} for merging method mappings for the merge session. Defaults to {@link FieldMergeStrategy#LOOSE}.
         *
         * @param fieldMergeStrategy The field merge strategy to use. Must not be {@code null}.
         * @return {@code this} instance for chaining.
         * @see MergeConfig#getFieldMergeStrategy()
         */
        public Builder withFieldMergeStrategy(final FieldMergeStrategy fieldMergeStrategy) {
            this.fieldMergeStrategy = Objects.requireNonNull(fieldMergeStrategy);
            return this;
        }

        /**
         * Set the parallelism for the {@link java.util.concurrent.Executors#newWorkStealingPool(int) work stealing pool} for the merge session.
         * Defaults to {@code -1}, which means to use {@link java.util.concurrent.Executors#newWorkStealingPool() Executors.newWorkStealingPool()}
         * to create the work stealing pool instead. Providing any value {@code <= 0} to this method will reset it back to the default value.
         *
         * @param parallelism The parallelism to use for the work stealing pool for the merge session.
         * @return {@code this} instance for chaining.
         * @see MergeConfig#getParallelism()
         * @since 0.5.6
         */
        public Builder withParallelism(final int parallelism) {
            if (parallelism <= 0) {
                this.parallelism = -1;
            } else {
                this.parallelism = parallelism;
            }
            return this;
        }

        /**
         * Create the {@link MergeConfig} from this object.
         *
         * @return The merge config created from this builder. Never {@code null}.
         */
        public MergeConfig build() {
            return new MergeConfig(this.handler, this.methodMergeStrategy, this.fieldMergeStrategy, this.parallelism);
        }
    }
}
