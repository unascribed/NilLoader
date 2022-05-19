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
 * Represents a unique, qualified path to an inner {@link ClassReference class}.
 *
 * @author Max Roncace
 * @since 0.3.1
 */
public class InnerClassReference extends ClassReference {

    /**
     * Derives the parent of an inner class based on its identifier.
     *
     * @param classType The full type of the inner class
     * @return A reference to the parent class
     * @throws IllegalArgumentException If the given type is not an inner class
     */
    private static ClassReference deriveParentClass(final ObjectType classType) throws IllegalArgumentException {
        if (classType.getClassName().indexOf(INNER_CLASS_SEPARATOR_CHAR) < 0) {
            throw new IllegalArgumentException("Cannot derive parent class from non-inner class identifier");
        }

        final ObjectType parentType = new ObjectType(
                classType.getClassName().substring(0, classType.getClassName().lastIndexOf('$'))
        );
        if (parentType.getClassName().indexOf(INNER_CLASS_SEPARATOR_CHAR) >= 0) {
            return new InnerClassReference(parentType);
        }
        else {
            return new TopLevelClassReference(parentType);
        }
    }

    private final ClassReference parentClass;

    /**
     * Constructs a new reference to an inner class.
     *
     * @param parentClass The parent class to the inner class
     * @param classType The full type of the inner class
     */
    InnerClassReference(final ClassReference parentClass, final ObjectType classType) {
        super(Type.INNER_CLASS, classType);

        assert(classType.getClassName().substring(0, classType.getClassName().lastIndexOf(INNER_CLASS_SEPARATOR_CHAR))
                        .equals(parentClass.classType.getClassName()));

        this.parentClass = parentClass;
    }

    /**
     * Constructs a new reference to an inner class.
     *
     * @param classType The full type of the inner class
     */
    public InnerClassReference(final ObjectType classType) {
        this(deriveParentClass(classType), classType);
    }

    /**
     * Gets a reference to the parent class of this inner class.
     *
     * @return The parent class
     */
    public ClassReference getParentClass() {
        return this.parentClass;
    }

}
