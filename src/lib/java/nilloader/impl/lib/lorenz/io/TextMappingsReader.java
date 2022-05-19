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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import nilloader.impl.lib.lorenz.MappingSet;
import nilloader.impl.lib.lorenz.io.srg.SrgReader;
import nilloader.impl.lib.lorenz.io.srg.csrg.CSrgReader;
import nilloader.impl.lib.lorenz.io.srg.tsrg.TSrgReader;

/**
 * An implementation of {@link MappingsReader} designed to aid
 * with the implementation of mapping readers for text-based
 * mapping formats.
 *
 * @see SrgReader
 * @see CSrgReader
 * @see TSrgReader
 *
 * @author Jamie Mansfield
 * @since 0.4.0
 */
public abstract class TextMappingsReader extends MappingsReader {

    protected final BufferedReader reader;
    protected final Function<MappingSet, Processor> processor;

    /**
     * Creates a new mappings reader, for the given {@link Reader}.
     *
     * @param reader The reader
     * @param processor The line processor to use for reading the lines
     */
    protected TextMappingsReader(Reader reader, final Function<MappingSet, Processor> processor) {
        this.reader = reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
        this.processor = processor;
    }

    @Override
    public MappingSet read(final MappingSet mappings) {
        final Processor processor = this.processor.apply(mappings);
        this.reader.lines()
                // Process line
                .forEach(processor);
        return mappings;
    }

    @Override
    public void close() throws IOException {
        this.reader.close();
    }

    /**
     * A parser for a given mappings format.
     *
     * @see SrgReader.Processor
     * @see CSrgReader.Processor
     * @see TSrgReader.Processor
     *
     * @since 0.4.0
     */
    public static abstract class Processor implements Consumer<String> {

        /**
         * A regular expression used to split {@link String}s at spaces.
         */
        protected static final Pattern SPACE = Pattern.compile(" ", Pattern.LITERAL);

        protected final MappingSet mappings;

        /**
         * Creates a mappings parser, to process the lines in a
         * mappings file.
         *
         * @param mappings The mappings set
         */
        protected Processor(final MappingSet mappings) {
            this.mappings = mappings;
        }

    }

}
