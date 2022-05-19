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

import java.util.ServiceLoader;

import nilloader.impl.lib.lorenz.util.Registry;

/**
 * A psuedo-enum of the mapping formats implemented within Lorenz.
 *
 * @author Jamie Mansfield
 * @since 0.4.0
 */
public final class MappingFormats {

    /**
     * The registry of {@link MappingFormat}s.
     */
    public static final Registry<MappingFormat> REGISTRY = new Registry<>();

    static {
        // Populate the registry
        for (final MappingFormat format : ServiceLoader.load(MappingFormat.class)) {
            REGISTRY.register(format.toString(), format);
        }
    }

    /**
     * The SRG mapping format.
     */
    public static final TextMappingFormat SRG = (TextMappingFormat) byId("srg");

    /**
     * The CSRG (compact SRG) mapping format.
     */
    public static final TextMappingFormat CSRG = (TextMappingFormat) byId("csrg");

    /**
     * The TSRG (tiny SRG) mapping format.
     */
    public static final TextMappingFormat TSRG = (TextMappingFormat) byId("tsrg");

    /**
     * The XSRG (SRG + field types) mapping format.
     */
    public static final TextMappingFormat XSRG = (TextMappingFormat) byId("xsrg");

    /**
     * @param id The identifier of the value
     * @return The value, or {@code null} if not present
     * @see Registry#byId(String)
     */
    public static MappingFormat byId(final String id) {
        return REGISTRY.byId(id);
    }

    private MappingFormats() {
    }

}
