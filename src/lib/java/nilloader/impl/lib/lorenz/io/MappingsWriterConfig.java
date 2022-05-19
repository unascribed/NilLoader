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

package nilloader.impl.lib.lorenz.io;

import nilloader.impl.lib.bombe.type.FieldType;
import nilloader.impl.lib.lorenz.model.ClassMapping;
import nilloader.impl.lib.lorenz.model.FieldMapping;
import nilloader.impl.lib.lorenz.model.Mapping;
import nilloader.impl.lib.lorenz.model.MethodMapping;

import java.util.Comparator;
import java.util.function.Function;

/**
 * Represents the configuration options for a {@link MappingsWriter mappings writer},
 * allowing consumers to fine-tune the output without regard to format.
 *
 * @author Jamie Mansfield
 * @since 0.5.5
 */
public class MappingsWriterConfig {

    /**
     * Creates a new builder for fluently constructing a writer
     * configuration.
     *
     * @return A builder
     */
    public static Builder builder() {
        return new Builder();
    }

    private final Comparator<ClassMapping<?, ?>> classMappingComparator;
    private final Comparator<FieldMapping> fieldMappingComparator;
    private final Comparator<MethodMapping> methodMappingComparator;

    MappingsWriterConfig(final Builder builder) {
        this.classMappingComparator = builder.classMappingComparator;
        this.fieldMappingComparator = builder.fieldMappingComparator;
        this.methodMappingComparator = builder.methodMappingComparator;
    }

    /**
     * Gets the class mapping comparator for arranging class mappings
     * when writing mappings.
     *
     * @return The class mappings comparator
     */
    public Comparator<ClassMapping<?, ?>> getClassMappingComparator() {
        return this.classMappingComparator;
    }

    /**
     * Gets the field mapping comparator for arranging field mappings
     * when writing mappings.
     *
     * @return The field mappings comparator
     */
    public Comparator<FieldMapping> getFieldMappingComparator() {
        return this.fieldMappingComparator;
    }

    /**
     * Gets the method mapping comparator for arranging method mappings
     * when writing mappings.
     *
     * @return The method mappings comparator
     */
    public Comparator<MethodMapping> getMethodMappingComparator() {
        return this.methodMappingComparator;
    }

    /**
     * Builder for fluently constructing a writer configuration.
     * <p>
     * There is a designated {@link #builder()} function intended for
     * creating a new builder - however consumers may also directly
     * construct a builder.
     */
    public static class Builder {

        private Comparator<ClassMapping<?, ?>> classMappingComparator =
                Utils.comparingLength(Mapping::getFullObfuscatedName);

        private Comparator<FieldMapping> fieldMappingComparator =
                Comparator.comparing(mapping -> mapping.getFullObfuscatedName() + mapping.getType().map(FieldType::toString).orElse(""));

        private Comparator<MethodMapping> methodMappingComparator =
                Comparator.comparing(mapping -> mapping.getFullObfuscatedName() + mapping.getDescriptor().toString());

        /**
         * Sets the class mapping comparator to be used for writing mappings.
         *
         * @param classMappingComparator The class mapping comparator
         * @throws NullPointerException If {@code classMappingComparator} is {@code null}
         * @return {@code this} for chaining
         */
        public Builder classMappingComparator(final Comparator<ClassMapping<?, ?>> classMappingComparator) {
            if (classMappingComparator == null) {
                throw new NullPointerException("classMappingComparator cannot be null!");
            }

            this.classMappingComparator = classMappingComparator;
            return this;
        }

        /**
         * Sets the field mapping comparator to be used for writing mappings.
         *
         * @param fieldMappingComparator The field mapping comparator
         * @throws NullPointerException If {@code fieldMappingComparator} is {@code null}
         * @return {@code this} for chaining
         */
        public Builder fieldMappingComparator(final Comparator<FieldMapping> fieldMappingComparator) {
            if (fieldMappingComparator == null) {
                throw new NullPointerException("fieldMappingComparator cannot be null!");
            }

            this.fieldMappingComparator = fieldMappingComparator;
            return this;
        }

        /**
         * Sets the method mapping comparator to be used for writing mappings.
         *
         * @param methodMappingComparator The method mapping comparator
         * @throws NullPointerException If {@code methodMappingComparator} is {@code null}
         * @return {@code this} for chaining
         */
        public Builder methodMappingComparator(final Comparator<MethodMapping> methodMappingComparator) {
            if (methodMappingComparator == null) {
                throw new NullPointerException("methodMappingComparator cannot be null!");
            }

            this.methodMappingComparator = methodMappingComparator;
            return this;
        }

        /**
         * Creates a writer configuration, using the values previously supplied.
         *
         * @return The writer configuration
         */
        public MappingsWriterConfig build() {
            return new MappingsWriterConfig(this);
        }

    }

    /**
     * Utility functions for assisting in the creation of a mappings
     * writer configuration.
     */
    public static class Utils {

        /**
         * Accepts a function that extracts a string sort key from a type {@code T},
         * and returns a {@link Comparator comparator} that compares using that key
         * when the lengths are both equal, and comparing based on length otherwise.
         *
         * @param keyExtractor The function used to extract the sort key
         * @param <T> The type of the object being compared
         * @return The comparator
         */
        public static <T> Comparator<T> comparingLength(final Function<? super T, String> keyExtractor) {
            return (c1, c2) -> {
                final String key1 = keyExtractor.apply(c1);
                final String key2 = keyExtractor.apply(c2);

                if (key1.length() != key2.length()) {
                    return key1.length() - key2.length();
                }

                return key1.compareTo(key2);
            };
        }

        private Utils() {
        }

    }

}
