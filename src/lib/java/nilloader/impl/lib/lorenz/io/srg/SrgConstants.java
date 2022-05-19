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

package nilloader.impl.lib.lorenz.io.srg;

import java.util.regex.Pattern;

/**
 * A collection of constants and utilities specific to
 * the SRG mapping format and its variants.
 *
 * @author Jamie Mansfield
 * @since 0.4.0
 */
public final class SrgConstants {

    /**
     * A regex expression used to remove comments from lines.
     */
    private static final Pattern HASH_COMMENT = Pattern.compile("#.*");

    /**
     * The standard file extension used with the SRG format.
     */
    public static final String STANDARD_EXTENSION = "srg";

    /**
     * Removes present comments, from the given {@link String} line.
     *
     * @param line The line
     * @return The comment-omitted line
     */
    public static String removeComments(final String line) {
        return HASH_COMMENT.matcher(line).replaceAll("");
    }

    /**
     * A collection of constants specific to the CSRG
     * mapping format.
     */
    public static final class CSrg {

        /**
         * The standard file extension used with the CSRG format.
         */
        public static final String STANDARD_EXTENSION = "csrg";

        private CSrg() {
        }

    }

    /**
     * A collection of constants specific to the TSRG
     * mapping format.
     */
    public static final class TSrg {

        /**
         * The standard file extension used with the TSRG format.
         */
        public static final String STANDARD_EXTENSION = "tsrg";

        private TSrg() {
        }

    }

    /**
     * A collection of constants specific to the XSRG
     * mapping format.
     *
     * @since 0.5.3
     */
    public static final class XSrg {

        /**
         * The standard file extension used with the TSRG format.
         */
        public static final String STANDARD_EXTENSION = "xsrg";

        private XSrg() {
        }

    }

    private SrgConstants() {
    }

}
