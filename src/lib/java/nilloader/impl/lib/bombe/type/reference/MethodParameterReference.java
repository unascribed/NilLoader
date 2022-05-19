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

/**
 * Represents a unique, qualified path to a parameter of a
 * {@link MethodReference method}.
 *
 * @author Max Roncace
 * @since 0.3.1
 */
public class MethodParameterReference extends QualifiedReference {

    private final MethodReference parentMethod;
    private final int index;

    /**
     * Constructs a new reference to a method parameter.
     *
     * @param parentMethod The method specifying the parameter
     * @param index The index of the parameter (0-indexed)
     * @throws IllegalArgumentException If the parameter index is out-of-bounds
     */
    public MethodParameterReference(final MethodReference parentMethod, final int index) throws IllegalArgumentException {
        super(Type.METHOD_PARAMETER);

        if (index >= parentMethod.getSignature().getDescriptor().getParamTypes().size()) {
            throw new IllegalArgumentException("Cannot get out-of-bounds parameter index " + index);
        }

        this.parentMethod = parentMethod;
        this.index = index;
    }

    /**
     * Gets the method specifying this parameter.
     *
     * @return The method specifying this parameter
     */
    public MethodReference getParentMethod() {
        return this.parentMethod;
    }

    /**
     * Gets the index of this parameter (0-indexed).
     *
     * @return The index of this parameter
     */
    public int getParameterIndex() {
        return this.index;
    }

    @Override
    public String toJvmsIdentifier() {
        return this.getParentMethod().toJvmsIdentifier() + JVMS_COMPONENT_JOINER + this.index;
    }

    @Override
    public StringJoiner buildToString() {
        return super.buildToString()
                    .add(";parentMethod=" + this.parentMethod.toJvmsIdentifier())
                    .add(";index=" + this.index);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MethodParameterReference)) return false;
        final MethodParameterReference that = (MethodParameterReference) obj;
        return super.equals(obj) &&
                Objects.equals(this.parentMethod, that.parentMethod) &&
                Objects.equals(this.index, that.index);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.parentMethod, this.index);
    }

}
