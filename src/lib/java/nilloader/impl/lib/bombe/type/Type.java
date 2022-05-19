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

import nilloader.impl.lib.bombe.analysis.InheritanceProvider;

/**
 * Represents a type within Java.
 *
 * @see BaseType
 * @see ObjectType
 * @see ArrayType
 * @see VoidType
 *
 * @author Jamie Mansfield
 * @since 0.1.0
 */
public interface Type {

    /**
     * Gets the appropriate {@link Type} for the given type.
     *
     * @param type The type
     * @return The type
     */
    static Type of(final String type) {
        return new TypeReader(type).readType();
    }

    /**
     * Gets the appropriate {@link Type} for the given class.
     *
     * @param klass The class
     * @return The type
     */
    static Type of(final Class<?> klass) {
        if (klass == Void.TYPE) {
            return VoidType.INSTANCE;
        }
        return FieldType.of(klass);
    }

    /**
     * Checks whether this type is assignable from the given {@link Type}, using
     * data provided by the given {@link InheritanceProvider}.
     *
     * @param that The type to check against
     * @param inheritanceProvider The inheritance provider
     * @return {@code true} if this type is assignable from the given type;
     *         {@code false} otherwise
     */
    default boolean isAssignableFrom(final Type that, final InheritanceProvider inheritanceProvider) {
        return this.equals(that);
    }

}
