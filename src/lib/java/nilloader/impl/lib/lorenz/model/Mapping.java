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

package nilloader.impl.lib.lorenz.model;

import java.util.Optional;
import java.util.function.Supplier;

import nilloader.impl.lib.lorenz.MappingSet;
import nilloader.impl.lib.lorenz.util.Reversible;

/**
 * Represents a de-obfuscation mapping for mappable constructs of the Java
 * class format - e.g. classes, fields, and methods.
 *
 * @param <M> The type of the mapping, used for chaining
 * @param <P> The type of the parent object
 *
 * @author Jamie Mansfield
 * @since 0.1.0
 */
public interface Mapping<M extends Mapping, P> extends Reversible<M, P> {

    /**
     * Gets the obfuscated name of the member being represented.
     *
     * @return The obfuscated name
     */
    String getObfuscatedName();

    /**
     * Gets the de-obfuscated name of the member being represented.
     * This can be altered through use of {@link #setDeobfuscatedName(String)}.
     *
     * @return The de-obfuscated name
     */
    String getDeobfuscatedName();

    /**
     * Sets the de-obfuscated name of the member.
     *
     * @param deobfuscatedName The new de-obfuscated name
     * @return {@code this} for chaining
     */
    M setDeobfuscatedName(final String deobfuscatedName);

    /**
     * Gets the unqualified ("simple") obfuscated name of the member.
     *
     * @return The simple obfuscated name
     * @since 0.4.0
     */
    default String getSimpleObfuscatedName() {
        return this.getObfuscatedName();
    }

    /**
     * Gets the unqualified ("simple") de-obfuscated name of the member.
     *
     * @return The simple de-obfuscated name
     * @since 0.4.0
     */
    default String getSimpleDeobfuscatedName() {
        return this.getDeobfuscatedName();
    }

    /**
     * Gets the fully-qualified obfuscated name of the member.
     *
     * @return The fully-qualified obfuscated name
     */
    String getFullObfuscatedName();

    /**
     * Gets the fully-qualified de-obfuscated name of the member.
     *
     * @return The fully-qualified de-obfuscated name
     */
    String getFullDeobfuscatedName();

    /**
     * Establishes whether the mapping has had a de-obfuscated name set.
     *
     * @return {@code True} if the mapping is mapped, {@code false} otherwise
     */
    boolean hasDeobfuscatedName();

    /**
     * Gets the {@link MappingSet} that the mappings belongs to.
     *
     * @return The owning mapping set
     */
    MappingSet getMappings();

    /**
     * Merges this mapping with another, to a given parent.
     *
     * @param with The mapping to merge with
     * @param parent The parent
     * @return The new mapping
     * @since 0.5.0
     */
    M merge(final M with, final P parent);

    /**
     * Clones this mapping, to a given parent.
     *
     * @param parent The parent
     * @return The cloned mapping
     * @since 0.5.0
     */
    M copy(final P parent);

    /**
     * Gets the value of the extension data, if it exists.
     *
     * @param key The key of the extension data
     * @param <T> The type of the extension data
     * @return The value, wrapped in an {@link Optional}
     * @since 0.5.0
     */
    <T> Optional<T> get(final ExtensionKey<T> key);

    /**
     * Gets the value of the extension data, or uses the supplier (setting that value too).
     *
     * @param key The key of the extension data
     * @param supplier The "backup" value supplier
     * @param <T> The type of the extension data
     * @return The value
     * @since 0.5.0
     */
    default <T> T getOrCreate(final ExtensionKey<T> key, final Supplier<T> supplier) {
        return this.get(key).orElseGet(() -> {
           final T value = supplier.get();
           this.set(key, value);
           return value;
        });
    }

    /**
     * Sets the given extension data to the given value.
     *
     * @param key The extension data key
     * @param value The extension data value
     * @param <T> The type of the extension data
     * @since 0.5.0
     */
    <T> void set(final ExtensionKey<T> key, final T value);

}
