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

import java.util.StringJoiner;

/**
 * All members within Java have a unique signature that they can be identified with,
 * classes that inherit from this class are a representation of those unique signatures.
 *
 * @see FieldSignature
 * @see MethodSignature
 *
 * @author Jamie Mansfield
 * @since 0.1.0
 */
public abstract class MemberSignature {

    protected final String name;

    /**
     * Creates a member signature, with the given name.
     *
     * @param name The name of the member
     */
    protected MemberSignature(final String name) {
        this.name = name;
    }

    /**
     * Gets the name of the member.
     *
     * @return The name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns a JVMS-like identifier corresponding to this signature.
     *
     * <p>
     *     For field signatures, this will take the form
     *     {@code name(descriptor)}.
     * </p>
     *
     * <p>
     *     For method signatures, this will take the form
     *     {@code name(params)ret_type} - in other terms, the name directly
     *     concatenated with the JVMS descriptor.
     * </p>
     *
     * @return A JVMS-like identifier
     */
    public abstract String toJvmsIdentifier();

    protected StringJoiner buildToString() {
        return new StringJoiner(", ", getClass().getSimpleName() + "{", "}")
                .add("name=" + name);
    }

    @Override
    public final String toString() {
        return this.buildToString().toString();
    }

}
