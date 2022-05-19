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
import nilloader.impl.lib.lorenz.MappingSet;
import nilloader.impl.lib.lorenz.model.FieldMapping;
import nilloader.impl.lib.lorenz.model.Mapping;
import nilloader.impl.lib.lorenz.model.MethodMapping;

import java.io.Closeable;
import java.io.IOException;
import java.util.Comparator;
import java.util.function.Function;

/**
 * Represents a writer, that is capable of writing de-obfuscation
 * mappings.
 * <p>
 * Each mappings writer will be designed for a specific mapping
 * format, and intended to be used with try-for-resources.
 *
 * @see TextMappingsWriter
 * @see BinaryMappingsWriter
 *
 * @author Jamie Mansfield
 * @since 0.4.0
 */
public abstract class MappingsWriter implements Closeable {

    /**
     * A {@link Comparator} used to alphabetise a collection of {@link Mapping}s.
     *
     * @deprecated 0.5.5 Use {@link #getConfig()} {@link MappingsWriterConfig#getClassMappingComparator()}
     *             instead
     */
    @Deprecated
    protected static final Comparator<Mapping> ALPHABETISE_MAPPINGS =
            comparingLength(Mapping::getFullObfuscatedName);

    /**
     * A {@link Comparator} used to alphabetise a collection of {@link FieldMapping}s.
     *
     * @since 0.5.0
     * @deprecated 0.5.5 Use {@link #getConfig()} {@link MappingsWriterConfig#getFieldMappingComparator()}
     *             instead
     */
    @Deprecated
    protected static final Comparator<FieldMapping> ALPHABETISE_FIELDS =
            Comparator.comparing(mapping -> mapping.getFullObfuscatedName() + mapping.getType().map(FieldType::toString).orElse(""));

    /**
     * A {@link Comparator} used to alphabetise a collection of {@link MethodMapping}s.
     *
     * @since 0.5.0
     * @deprecated 0.5.5 Use {@link #getConfig()} {@link MappingsWriterConfig#getMethodMappingComparator()}
     *             instead
     */
    @Deprecated
    protected static final Comparator<MethodMapping> ALPHABETISE_METHODS =
            Comparator.comparing(mapping -> mapping.getFullObfuscatedName() + mapping.getDescriptor().toString());

    private static <T> Comparator<T> comparingLength(final Function<? super T, String> keyExtractor) {
        return (c1, c2) -> {
            final String key1 = keyExtractor.apply(c1);
            final String key2 = keyExtractor.apply(c2);
            if (key1.length() != key2.length()) {
                return key1.length() - key2.length();
            }
            return key1.compareTo(key2);
        };
    }

    protected MappingsWriterConfig config = MappingsWriterConfig.builder().build();

    /**
     * Gets the active {@link MappingsWriterConfig writer configuration} for
     * this mappings writer.
     *
     * @return The writer configuration
     * @since 0.5.5
     */
    public MappingsWriterConfig getConfig() {
        return this.config;
    }

    /**
     * Sets the active {@link MappingsWriterConfig writer configuration} for
     * this mappings writer - allowing the output of the writer to be fine-tuned
     * to fit the environment in use.
     *
     * @param config The writer configuration
     * @throws NullPointerException If {@code config} is {@code null}
     * @since 0.5.5
     */
    public void setConfig(final MappingsWriterConfig config) {
        if (config == null) {
            throw new NullPointerException("config cannot be null!");
        }

        this.config = config;
    }

    /**
     * Writes the given mappings to the previously given output.
     *
     * @param mappings The mapping set
     * @throws IOException Should an IO issue occur
     */
    public abstract void write(final MappingSet mappings) throws IOException;

}
