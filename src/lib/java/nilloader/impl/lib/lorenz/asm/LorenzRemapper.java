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

package nilloader.impl.lib.lorenz.asm;

import nilloader.api.lib.asm.commons.Remapper;
import nilloader.impl.lib.bombe.analysis.InheritanceProvider;
import nilloader.impl.lib.bombe.type.signature.FieldSignature;
import nilloader.impl.lib.bombe.type.signature.MethodSignature;
import nilloader.impl.lib.lorenz.MappingSet;
import nilloader.impl.lib.lorenz.model.ClassMapping;
import nilloader.impl.lib.lorenz.model.Mapping;

/**
 * A simple implementation of {@link Remapper} to remap based
 * on a {@link MappingSet}.
 *
 * <p>Note: The implementation modifies the given {@link MappingSet}
 * on demand to complete the mappings with missing mappings inherited
 * from parent classes.</p>
 *
 * @author Jamie Mansfield
 * @since 0.4.0
 */
public class LorenzRemapper extends Remapper {

    private final MappingSet mappings;
    private final InheritanceProvider inheritanceProvider;

    public LorenzRemapper(final MappingSet mappings, final InheritanceProvider inheritanceProvider) {
        this.mappings = mappings;
        this.inheritanceProvider = inheritanceProvider;
    }

    @Override
    public String map(final String typeName) {
        return this.mappings.computeClassMapping(typeName)
                .map(Mapping::getFullDeobfuscatedName)
                .orElse(typeName);
    }

    @Override
    public String mapInnerClassName(final String name, final String ownerName, final String innerName) {
        return this.mappings.computeClassMapping(name)
                .map(Mapping::getDeobfuscatedName)
                .orElse(innerName);
    }

    private ClassMapping<?, ?> getCompletedClassMapping(final String owner) {
        final ClassMapping<?, ?> mapping = this.mappings.getOrCreateClassMapping(owner);
        mapping.complete(this.inheritanceProvider);
        return mapping;
    }

    @Override
    public String mapFieldName(final String owner, final String name, final String desc) {
        return this.getCompletedClassMapping(owner)
                .computeFieldMapping(FieldSignature.of(name, desc))
                .map(Mapping::getDeobfuscatedName)
                .orElse(name);
    }

    @Override
    public String mapMethodName(final String owner, final String name, final String desc) {
        return this.getCompletedClassMapping(owner)
                .getMethodMapping(MethodSignature.of(name, desc))
                .map(Mapping::getDeobfuscatedName)
                .orElse(name);
    }

}
