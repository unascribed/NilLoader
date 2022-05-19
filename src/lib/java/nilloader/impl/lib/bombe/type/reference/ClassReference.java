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

import java.util.Objects;
import java.util.StringJoiner;

import nilloader.impl.lib.bombe.type.FieldType;
import nilloader.impl.lib.bombe.type.MethodDescriptor;
import nilloader.impl.lib.bombe.type.ObjectType;
import nilloader.impl.lib.bombe.type.signature.FieldSignature;
import nilloader.impl.lib.bombe.type.signature.MethodSignature;

/**
 * Represents a unique, qualified path to a class.
 *
 * @author Max Roncace
 * @since 0.3.1
 */
public abstract class ClassReference extends QualifiedReference {

    protected static final char INNER_CLASS_SEPARATOR_CHAR = '$';

    protected final ObjectType classType;

    /**
     * Constructs a new reference to a class.
     *
     * @param type The {@link Type} of reference (must be
     *     {@link Type#TOP_LEVEL_CLASS} or {@link Type#INNER_CLASS})
     * @param classType An {@link ObjectType} representing the type of the
     *     referenced class
     */
    public ClassReference(final Type type, final ObjectType classType) {
        super(type);
        this.classType = classType;
    }

    /**
     * Returns the type of class represented by this reference
     *
     * @return The type of class
     */
    public ObjectType getClassType() {
        return this.classType;
    }

    /**
     * Returns a reference to an inner class of this class.
     *
     * @param unqualifiedName The unqualified name of the inner class
     * @return A reference to the inner class
     */
    public InnerClassReference getInnerClass(final String unqualifiedName) {
        return new InnerClassReference(
                this,
                new ObjectType(this.getClassType().getClassName() + INNER_CLASS_SEPARATOR_CHAR + unqualifiedName)
        );
    }

    /**
     * Returns a reference to a field contained by this class.
     *
     * @param signature The {@link FieldSignature signature} of the field
     * @return A reference to the field
     */
    public FieldReference getField(final FieldSignature signature) {
        return new FieldReference(this, signature);
    }

    /**
     * Returns a reference to a field contained by this class.
     *
     * @param name The name of the field
     * @param type The {@link FieldType type} of the field
     * @return A reference to the field
     */
    public FieldReference getField(final String name, final FieldType type) {
        return this.getField(new FieldSignature(name, type));
    }

    /**
     * Returns a reference to a field contained by this class.
     *
     * @param name The name of the field
     * @param type The type of the field
     * @return A reference to the field
     */
    public FieldReference getField(final String name, final String type) {
        return this.getField(name, FieldType.of(type));
    }

    /**
     * Returns a reference to a field contained by this class.
     *
     * @param name The name of the field
     * @return A reference to the field
     */
    public FieldReference getField(final String name) {
        return this.getField(new FieldSignature(name));
    }

    /**
     * Returns a reference to a method contained by this class.
     *
     * @param signature The {@link MethodSignature signature} of the method
     * @return A reference to the method
     */
    public MethodReference getMethod(final MethodSignature signature) {
        return new MethodReference(this, signature);
    }

    /**
     * Returns a reference to a method contained by this class.
     *
     * @param name The name of the method
     * @param descriptor The {@link MethodDescriptor descriptor} of the method
     * @return A reference to the method
     */
    public MethodReference getMethod(final String name, final MethodDescriptor descriptor) {
        return this.getMethod(new MethodSignature(name, descriptor));
    }

    /**
     * Returns a reference to a method contained by this class.
     *
     * @param name The name of the method
     * @param descriptor The JVMS descriptor of the method
     * @return A reference to the method
     */
    public MethodReference getMethod(final String name, final String descriptor) {
        return this.getMethod(name, MethodDescriptor.of(descriptor));
    }

    @Override
    public String toJvmsIdentifier() {
        return this.classType.getClassName();
    }

    @Override
    protected StringJoiner buildToString() {
        return super.buildToString().add(";classType=" + this.classType.getClassName());
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ClassReference)) return false;
        final ClassReference that = (ClassReference) obj;
        return Objects.equals(this.classType, that.classType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.classType);
    }

}
