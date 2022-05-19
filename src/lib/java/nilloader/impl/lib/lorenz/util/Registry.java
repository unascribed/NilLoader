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

package nilloader.impl.lib.lorenz.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * A registry of identifiable objects.
 *
 * @param <T> The type of the registered objects
 * @author Jamie Mansfield
 * @since 0.5.0
 */
public class Registry<T> {

    private final Map<String, T> map = new HashMap<>();

    /**
     * Registers the given value, with the given identifier.
     *
     * @param id The identifier of the value
     * @param value The value
     * @return {@code this}, for chaining
     */
    public Registry<T> register(final String id, final T value) {
        this.map.put(id.toLowerCase(), value);
        return this;
    }

    /**
     * Gets the value of the given identifier.
     *
     * @param id The identifier of the value
     * @return The value, or {@code null} if not present
     */
    public T byId(final String id) {
        return this.map.get(id.toLowerCase());
    }

    /**
     * Gets an <em>immutable</em>-view of value keys registered into
     * the registry.
     *
     * @return The keys
     * @since 0.5.3
     *
     * @see Map#keySet()
     */
    public Set<String> keys() {
        return Collections.unmodifiableSet(this.map.keySet());
    }

    /**
     * Gets an <em>immutable</em>-view of all values registered into
     * the registry.
     *
     * @return The values
     *
     * @see Map#values()
     */
    public Collection<T> values() {
        return Collections.unmodifiableCollection(this.map.values());
    }

    /**
     * Gets an <em>immutable</em>-view of all entries (key + value)
     * registered into the registry.
     *
     * @return The entries
     * @since 0.5.3
     *
     * @see Map#entrySet()
     */
    public Set<Map.Entry<String, T>> entries() {
        return Collections.unmodifiableSet(this.map.entrySet());
    }

    /**
     * @param consumer The action to be performed for each entry
     * @see Map#forEach(BiConsumer)
     */
    public void forEach(final BiConsumer<String, T> consumer) {
        this.map.forEach(consumer);
    }

}
