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

import java.util.Objects;
import java.util.StringJoiner;

/**
 * The object used as a key in Lorenz's extension data system, simply
 * used to hold the type and identifier.
 *
 * @param <T> The type of the data
 * @author Jamie Mansfield
 * @since 0.5.0
 */
public class ExtensionKey<T> {

    private final Class<T> type;
    private final String id;

    public ExtensionKey(final Class<T> type, final String id) {
        this.type = type;
        this.id = id;
    }

    /**
     * @param obj Object to be cast
     * @return Object cast to {@code T}
     * @see Class#cast(Object)
     */
    public T cast(final Object obj) {
        return this.type.cast(obj);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj) || !(obj instanceof ExtensionKey)) return false;
        final ExtensionKey that = (ExtensionKey) obj;
        return Objects.equals(this.type, that.type) &&
                Objects.equals(this.id, that.id);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", this.getClass().getSimpleName() + "{", "}")
                .add("type=" + this.type)
                .add("id=" + this.id)
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.type, this.id);
    }

}
