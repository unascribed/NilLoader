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

import nilloader.impl.lib.bombe.type.signature.MemberSignature;

/**
 * Represents a unique, qualified path to a {@link ClassReference class} member.
 *
 * @param <S> The {@link MemberSignature} type used by this reference
 *
 * @author Max Roncace
 * @since 0.3.1
 */
public abstract class MemberReference<S extends MemberSignature> extends QualifiedReference {

    protected final ClassReference owningClass;
    protected final S signature;

    /**
     * Constructs a new reference to a class member.
     *
     * @param type The type of reference (must be either {@link Type#FIELD} or
     * {@link Type#METHOD}.
     *
     * @param owningClass A reference to the class which owns the member
     * @param signature The signature of the member
     */
    public MemberReference(final Type type, final ClassReference owningClass, final S signature) {
        super(type);
        this.owningClass = owningClass;
        this.signature = signature;
    }

    /**
     * Gets the class which owns this member.
     *
     * @return The class which owns this member
     */
    public ClassReference getOwningClass() {
        return this.owningClass;
    }

    /**
     * Gets the signature of this member.
     *
     * @return The signature of this member
     */
    public S getSignature() {
        return this.signature;
    }

    @Override
    public String toJvmsIdentifier() {
        return this.owningClass.toJvmsIdentifier() + JVMS_COMPONENT_JOINER + this.signature.toJvmsIdentifier();
    }

    @Override
    protected StringJoiner buildToString() {
        return super.buildToString()
                    .add(";owningClass=" + this.owningClass.getClassType().getClassName())
                    .add(";signature=" + this.signature.toJvmsIdentifier());
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MemberReference)) return false;
        final MemberReference that = (MemberReference) obj;
        return super.equals(obj) &&
                Objects.equals(this.owningClass, that.owningClass) &&
                Objects.equals(this.signature, that.signature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.owningClass, this.signature);
    }

}
