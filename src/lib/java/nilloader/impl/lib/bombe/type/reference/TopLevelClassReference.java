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

package nilloader.impl.lib.bombe.type.reference;

import nilloader.impl.lib.bombe.type.ObjectType;

/**
 * Represents a unique, qualified path to a top-level
 * {@link ClassReference class}.
 *
 * @author Max Roncace
 * @since 0.3.1
 */
public class TopLevelClassReference extends ClassReference {

    /**
     * Constructs a new reference to a top-level class.
     *
     * @param classType The type of the class
     * @throws IllegalArgumentException If the given type represents an inner
     *     class
     */
    public TopLevelClassReference(final ObjectType classType) {
        super(Type.TOP_LEVEL_CLASS, classType);

        if (classType.getClassName().indexOf(INNER_CLASS_SEPARATOR_CHAR) >= 0) {
            throw new IllegalArgumentException("Cannot create top-level class reference from inner class identifier");
        }
    }

    /**
     * Constructs a new reference to a top-level class.
     *
     * @param className The name of the class
     * @throws IllegalArgumentException If the given type represents an inner
     *     class
     */
    public TopLevelClassReference(final String className) {
        this(new ObjectType(className));
    }

}
