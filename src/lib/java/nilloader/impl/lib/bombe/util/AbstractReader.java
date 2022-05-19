/*
 * Copyright (c) 2018, Jamie Mansfield <https://jamiemansfield.me/>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *  Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package nilloader.impl.lib.bombe.util;

/**
 * An abstract reader for reading from a {@link String} source.
 *
 * @author Jamie Mansfield
 * @since 0.2.0
 */
public abstract class AbstractReader {

    protected final String source;
    protected int current = 0;

    /**
     * Creates a new reader, from the given source material.
     *
     * @param source The source to read from
     */
    protected AbstractReader(final String source) {
        this.source = source;
    }

    /**
     * Establishes whether there remains to be something to read.
     *
     * @return {@code true} if there is something to read;
     *         {@code false} otherwise
     */
    public boolean hasNext() {
        return this.current < this.source.length();
    }

    /**
     * Gets the next character to be read, without consuming it.
     *
     * @return The next character
     * @throws IllegalStateException If there is no character available
     */
    protected char peek() {
        if (!this.hasNext()) throw new IllegalStateException("No character available to peek at!");
        return this.source.charAt(this.current);
    }

    /**
     * Gets the previous character read.
     *
     * @return The next character
     * @throws IllegalStateException If there is no character available
     */
    protected char previous() {
        if (this.current <= 0) throw new IllegalStateException("No previous character available!");
        return this.source.charAt(this.current - 1);
    }

    /**
     * Gets the next character to be read, consuming it.
     *
     * @return The next character
     * @throws IllegalStateException If there is no character available
     */
    protected char advance() {
        if (!this.hasNext()) throw new IllegalStateException("No character available to advance to!");
        return this.source.charAt(this.current++);
    }

}
