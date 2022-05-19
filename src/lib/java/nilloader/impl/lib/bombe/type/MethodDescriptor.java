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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A model of a method descriptor, a text representation of a method's
 * parameter type and return type.
 *
 * <p>The format is simply {@code "(ParamTypes...)ReturnType"}, for example
 * given a method with two integer parameters and a {@link String} return
 * type - the descriptor would be {@code "(II)Ljava/lang/String;"}.</p>
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.3.3">Method Descriptors</a>
 *
 * @author Jamie Mansfield
 * @since 0.1.0
 */
public final class MethodDescriptor {

    private final List<FieldType> paramTypes;
    private final Type returnType;

    /**
     * Compiles a {@link MethodDescriptor} for the given raw descriptor.
     *
     * @param descriptor The raw method descriptor
     * @return The descriptor
     */
    public static MethodDescriptor of(final String descriptor) {
        return new MethodDescriptorReader(descriptor).read();
    }

    /**
     * Creates a {@link MethodDescriptor} for the given method.
     *
     * @param method The method
     * @return The descriptor
     * @since 0.3.0
     */
    public static MethodDescriptor of(final Method method) {
        return new MethodDescriptor(
                Arrays.stream(method.getParameterTypes()).map(FieldType::of).collect(Collectors.toList()),
                Type.of(method.getReturnType())
        );
    }

    /**
     * Creates a descriptor from the given param types, and return type.
     *
     * @param paramTypes The parameter types of the method
     * @param returnType The return type of the method
     */
    public MethodDescriptor(final List<FieldType> paramTypes, final Type returnType) {
        this.paramTypes = paramTypes;
        this.returnType = returnType;
    }

    /**
     * Gets an immutable-view of the parameter {@link Type}s of the
     * method.
     *
     * @return The method's param types
     */
    public List<FieldType> getParamTypes() {
        return Collections.unmodifiableList(this.paramTypes);
    }

    /**
     * Gets the return {@link Type} of the method.
     *
     * @return The method's return type
     */
    public Type getReturnType() {
        return this.returnType;
    }

    @Override
    public String toString() {
        final StringBuilder typeBuilder = new StringBuilder();

        typeBuilder.append("(");
        this.paramTypes.forEach(type -> typeBuilder.append(type.toString()));
        typeBuilder.append(")");
        typeBuilder.append(this.returnType.toString());

        return typeBuilder.toString();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MethodDescriptor)) return false;
        final MethodDescriptor that = (MethodDescriptor) obj;
        return Objects.equals(this.paramTypes, that.paramTypes) &&
                Objects.equals(this.returnType, that.returnType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.paramTypes, this.returnType);
    }

}
