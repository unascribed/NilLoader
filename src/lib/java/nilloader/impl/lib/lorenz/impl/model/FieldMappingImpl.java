/*
 * This file is part of Lorenz, licensed under the MIT License (MIT).
 *
 * Copyright (c) Jamie Mansfield <https://www.jamierocks.uk/>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package nilloader.impl.lib.lorenz.impl.model;

import nilloader.impl.lib.bombe.type.signature.FieldSignature;
import nilloader.impl.lib.lorenz.model.ClassMapping;
import nilloader.impl.lib.lorenz.model.FieldMapping;

import java.util.Objects;

/**
 * A basic implementation of {@link FieldMapping}.
 *
 * @author Jamie Mansfield
 * @since 0.2.0
 */
public class FieldMappingImpl
        extends AbstractMemberMappingImpl<FieldMapping, ClassMapping>
        implements FieldMapping {

    private final FieldSignature signature;

    /**
     * Creates a new field mapping, from the given parameters.
     *
     * @param parentClass The class mapping, this mapping belongs to
     * @param signature The obfuscated signature
     * @param deobfuscatedName The de-obfuscated name
     */
    public FieldMappingImpl(final ClassMapping parentClass, final FieldSignature signature, final String deobfuscatedName) {
        super(parentClass, signature.getName(), deobfuscatedName);
        this.signature = signature;
    }

    @Override
    public FieldSignature getSignature() {
        return this.signature;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj) || !(obj instanceof FieldMapping)) return false;
        final FieldMapping that = (FieldMapping) obj;
        return Objects.equals(this.signature, that.getSignature());
    }

}
