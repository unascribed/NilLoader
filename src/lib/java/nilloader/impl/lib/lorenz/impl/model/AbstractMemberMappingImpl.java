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

import java.util.Objects;

import nilloader.impl.lib.lorenz.model.Mapping;
import nilloader.impl.lib.lorenz.model.MemberMapping;

/**
 * An abstract basic implementation of {@link MemberMapping}.
 *
 * @param <M> The type of the mapping
 * @param <P> The type of the parent mapping
 *
 * @author Jamie Mansfield
 * @since 0.2.0
 */
public abstract class AbstractMemberMappingImpl<M extends MemberMapping, P extends Mapping>
        extends AbstractMappingImpl<M, P>
        implements MemberMapping<M, P> {

    private final P parent;

    /**
     * Creates a new member mapping, from the given parameters.
     *
     * @param parent The mapping, this mapping belongs to
     * @param obfuscatedName The obfuscated name
     * @param deobfuscatedName The de-obfuscated name
     */
    protected AbstractMemberMappingImpl(final P parent, final String obfuscatedName,
            final String deobfuscatedName) {
        super(parent.getMappings(), obfuscatedName, deobfuscatedName);
        this.parent = parent;
    }

    @Override
    public P getParent() {
        return this.parent;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (!(obj instanceof MemberMapping)) return false;

        final MemberMapping that = (MemberMapping) obj;
        return Objects.equals(this.parent, that.getParent());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.parent.getFullObfuscatedName(), this.parent.getFullDeobfuscatedName());
    }

}
