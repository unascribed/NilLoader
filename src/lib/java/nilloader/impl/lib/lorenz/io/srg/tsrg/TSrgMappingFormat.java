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

package nilloader.impl.lib.lorenz.io.srg.tsrg;

import java.io.Reader;
import java.io.Writer;
import java.util.Optional;

import nilloader.impl.lib.lorenz.io.MappingsReader;
import nilloader.impl.lib.lorenz.io.MappingsWriter;
import nilloader.impl.lib.lorenz.io.TextMappingFormat;
import nilloader.impl.lib.lorenz.io.srg.SrgConstants;

/**
 * The TSRG mapping format.
 *
 * @author Jamie Mansfield
 * @since 0.4.0
 */
public class TSrgMappingFormat implements TextMappingFormat {

    @Override
    public MappingsReader createReader(final Reader reader) {
        return new TSrgReader(reader);
    }

    @Override
    public MappingsWriter createWriter(final Writer writer) {
        return new TSrgWriter(writer);
    }

    @Override
    public Optional<String> getStandardFileExtension() {
        return Optional.of(SrgConstants.TSrg.STANDARD_EXTENSION);
    }

    @Override
    public String toString() {
        return "tsrg";
    }

}
