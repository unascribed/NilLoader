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

/**
 * Represents any type that can be used in a field.
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-FieldType">FieldType</a>
 * @see BaseType
 * @see ObjectType
 * @see ArrayType
 *
 * @author Jamie Mansfield
 * @since 0.1.0
 */
public interface FieldType extends Type {

    /**
     * Gets the appropriate {@link FieldType} for the given type.
     *
     * @param type The field type
     * @return The field type
     */
    static FieldType of(final String type) {
        return new TypeReader(type).readFieldType();
    }

    /**
     * Gets the appropriate {@link FieldType} for the given class.
     *
     * @param klass The class
     * @return The field type
     */
    static FieldType of(final Class<?> klass) {
        if (klass.isPrimitive()) {
            if (klass == Boolean.TYPE) {
                return BaseType.BOOLEAN;
            }
            else if (klass == Character.TYPE) {
                return BaseType.CHAR;
            }
            else if (klass == Byte.TYPE) {
                return BaseType.BYTE;
            }
            else if (klass == Short.TYPE) {
                return BaseType.SHORT;
            }
            else if (klass == Integer.TYPE) {
                return BaseType.INT;
            }
            else if (klass == Long.TYPE) {
                return BaseType.LONG;
            }
            else if (klass == Float.TYPE) {
                return BaseType.FLOAT;
            }
            else if (klass == Double.TYPE) {
                return BaseType.DOUBLE;
            }
            else {
                throw new RuntimeException("Invalid base type: " + klass.getName());
            }
        }
        else if (klass.isArray()) {
            int dimensions = 0;
            Class<?> componentType = klass;
            do {
                componentType = componentType.getComponentType();
                dimensions++;
            } while (componentType.isArray());
            return new ArrayType(dimensions, of(componentType));
        }
        else {
            return new ObjectType(klass.getName());
        }
    }

}
