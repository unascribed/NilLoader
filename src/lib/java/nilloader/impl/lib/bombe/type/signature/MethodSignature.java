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

package nilloader.impl.lib.bombe.type.signature;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.StringJoiner;

import nilloader.impl.lib.bombe.type.MethodDescriptor;

/**
 * Represents a method within a class, by its name and descriptor.
 *
 * @author Jamie Mansfield
 * @since 0.1.0
 */
public class MethodSignature extends MemberSignature {

    private final MethodDescriptor descriptor;

    /**
     * Creates a method signature, with the given method name and raw descriptor.
     *
     * @param name The method name
     * @param descriptor The method's raw descriptor
     * @return The new method signature
     */
    public static MethodSignature of(final String name, final String descriptor) {
        return new MethodSignature(name, MethodDescriptor.of(descriptor));
    }

    /**
     * Creates a method signature, with the given raw string that contains the
     * method name and descriptor concatenated.
     *
     * @param nameAndDescriptor The method name and descriptor
     * @return The new method signature
     */
    public static MethodSignature of(final String nameAndDescriptor) {
        int methodIndex = nameAndDescriptor.indexOf('(');
        return of(nameAndDescriptor.substring(0, methodIndex), nameAndDescriptor.substring(methodIndex));
    }

    /**
     * Creates a {@link MethodSignature} for the given method.
     *
     * @param method The method
     * @return The signature
     * @since 0.3.0
     */
    public static MethodSignature of(final Method method) {
        return new MethodSignature(method.getName(), MethodDescriptor.of(method));
    }

    /**
     * Creates a method signature, with the given name and {@link MethodDescriptor}.
     *
     * @param name The method name
     * @param descriptor The method descriptor
     */
    public MethodSignature(final String name, final MethodDescriptor descriptor) {
        super(name);
        this.descriptor = descriptor;
    }

    /**
     * Gets the descriptor of the method.
     *
     * @return The descriptor
     */
    public MethodDescriptor getDescriptor() {
        return this.descriptor;
    }

    @Override
    public String toJvmsIdentifier() {
        return this.name + this.descriptor.toString();
    }

    @Override
    protected StringJoiner buildToString() {
        return super.buildToString()
                .add("descriptor=" + this.descriptor);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MethodSignature)) return false;
        final MethodSignature that = (MethodSignature) obj;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.descriptor, that.descriptor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.descriptor);
    }

}
