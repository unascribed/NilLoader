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
 * Represents a unique, qualified path to a class, class member, or method
 * parameter.
 *
 * @author Max Roncace
 * @since 0.3.1
 */
public abstract class QualifiedReference {

    protected static final char JVMS_COMPONENT_JOINER = '.';

    protected final Type type;

    public QualifiedReference(final Type type) {
        this.type = type;
    }

    /**
     * Returns the {@link Type} of this reference.
     *
     * @return The {@link Type} of this reference
     */
    public Type getType() {
        return this.type;
    }

    /**
     * Returns a JVMS-like identifier string corresponding to this reference.
     *
     * The JVMS does not specify a qualified format for member and parameter
     * identifiers, so for these cases, a dot (".") is used to separate the
     * class, member signature, and parameter index components (as appropriate).
     *
     * @return A JVMS-like identifier string for this reference
     *
     * @see <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.2"></a>
     * @see <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.3"></a>
     */
    public abstract String toJvmsIdentifier();

    protected StringJoiner buildToString() {
        return new StringJoiner("{type=" + this.type.name());
    }

    @Override
    public String toString() {
        return buildToString().add("}").toString();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof QualifiedReference)) return false;
        final QualifiedReference that = (QualifiedReference) obj;
        return Objects.equals(this.type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.type);
    }

    public enum Type {
        TOP_LEVEL_CLASS,
        INNER_CLASS,
        FIELD,
        METHOD,
        METHOD_PARAMETER
    }

}
