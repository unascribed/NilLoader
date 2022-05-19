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

package nilloader.impl.lib.bombe.type;

import java.util.Arrays;

/**
 * Represents a base type within Java.
 *
 * @see <a href="http://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-BaseType">BaseType</a>
 *
 * @author Jamie Mansfield
 * @since 0.1.0
 */
public enum BaseType implements PrimitiveType, FieldType {

    BYTE('B'),
    CHAR('C'),
    DOUBLE('D'),
    FLOAT('F'),
    INT('I'),
    LONG('J'),
    SHORT('S'),
    BOOLEAN('Z'),
    ;

    private final char key;
    private final String descriptor;

    /**
     * Creates a new base type, with the given character type.
     *
     * @param key The character key
     */
    BaseType(final char key) {
        this.key = key;
        this.descriptor = String.valueOf(key);
    }

    @Override
    public char getKey() {
        return this.key;
    }

    @Override
    public String toString() {
        return this.descriptor;
    }

    /**
     * Establishes whether the given key, is a valid base
     * key.
     *
     * @param key The key
     * @return {@code true} if the key represents a base type;
     *         {@code false} otherwise
     */
    public static boolean isValidBase(final char key) {
        return Arrays.stream(values())
                .anyMatch(type -> type.key == key);
    }

    /**
     * Gets the {@link BaseType} from the given key.
     *
     * @param key The key
     * @return The base type
     */
    public static BaseType getFromKey(final char key) {
        return Arrays.stream(values())
                .filter(type -> type.key == key)
                .findFirst().orElse(null);
    }

}
