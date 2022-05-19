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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import nilloader.impl.lib.lorenz.io.srg.SrgWriter;
import nilloader.impl.lib.lorenz.io.srg.csrg.CSrgWriter;
import nilloader.impl.lib.lorenz.io.srg.tsrg.TSrgWriter;

/**
 * An implementation of {@link MappingsWriter} designed to aid
 * with the implementation of mapping writers for text-based
 * mapping formats.
 *
 * @see SrgWriter
 * @see CSrgWriter
 * @see TSrgWriter
 *
 * @author Jamie Mansfield
 * @since 0.4.0
 */
public abstract class TextMappingsWriter extends MappingsWriter {

    protected final PrintWriter writer;

    /**
     * Creates a new mappings writer, from the given {@link Writer}.
     *
     * @param writer The output writer, to write to
     */
    protected TextMappingsWriter(final Writer writer) {
        if (writer instanceof PrintWriter) {
            this.writer = (PrintWriter) writer;
        } else {
            this.writer = new PrintWriter(writer);
        }
    }

    @Override
    public void close() throws IOException {
        this.writer.flush();
        this.writer.close();
    }

}
