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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

import nilloader.impl.lib.lorenz.MappingSet;

/**
 * Represents a reader that reads de-obfuscation mappings.
 *
 * @see TextMappingsReader
 * @see BinaryMappingsReader
 *
 * @author Jamie Mansfield
 * @since 0.4.0
 */
public abstract class MappingsReader implements Closeable {

    /**
     * Reads mappings from the previously given {@link InputStream}, to
     * a new {@link MappingSet}.
     *
     * @return The mapping set
     * @throws IOException Should an I/O issue occur
     */
    public MappingSet read() throws IOException {
        return this.read(MappingSet.create());
    }

    /**
     * Reads mappings from the previously given {@link InputStream}, to
     * the given {@link MappingSet}.
     *
     * @param mappings The mapping set
     * @return The mapping set, to allow for chaining
     * @throws IOException Should an I/O issue occur
     */
    public abstract MappingSet read(final MappingSet mappings) throws IOException;

}
