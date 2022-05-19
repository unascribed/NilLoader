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

package nilloader.impl.lib.lorenz.impl;

import nilloader.impl.lib.bombe.type.signature.FieldSignature;
import nilloader.impl.lib.bombe.type.signature.MethodSignature;
import nilloader.impl.lib.lorenz.MappingSet;
import nilloader.impl.lib.lorenz.MappingSetModelFactory;
import nilloader.impl.lib.lorenz.impl.model.FieldMappingImpl;
import nilloader.impl.lib.lorenz.impl.model.InnerClassMappingImpl;
import nilloader.impl.lib.lorenz.impl.model.MethodMappingImpl;
import nilloader.impl.lib.lorenz.impl.model.MethodParameterMappingImpl;
import nilloader.impl.lib.lorenz.impl.model.TopLevelClassMappingImpl;
import nilloader.impl.lib.lorenz.model.ClassMapping;
import nilloader.impl.lib.lorenz.model.FieldMapping;
import nilloader.impl.lib.lorenz.model.InnerClassMapping;
import nilloader.impl.lib.lorenz.model.MethodMapping;
import nilloader.impl.lib.lorenz.model.MethodParameterMapping;
import nilloader.impl.lib.lorenz.model.TopLevelClassMapping;

/**
 * A basic implementation of {@link MappingSetModelFactory}.
 *
 * @author Jamie Mansfield
 * @since 0.3.0
 */
public class MappingSetModelFactoryImpl implements MappingSetModelFactory {

    @Override
    public TopLevelClassMapping createTopLevelClassMapping(final MappingSet parent, final String obfuscatedName, final String deobfuscatedName) {
        return new TopLevelClassMappingImpl(parent, obfuscatedName, deobfuscatedName);
    }

    @Override
    public InnerClassMapping createInnerClassMapping(final ClassMapping parent, final String obfuscatedName, final String deobfuscatedName) {
        return new InnerClassMappingImpl(parent, obfuscatedName, deobfuscatedName);
    }

    @Override
    public FieldMapping createFieldMapping(final ClassMapping parent, final FieldSignature signature, final String deobfuscatedName) {
        return new FieldMappingImpl(parent, signature, deobfuscatedName);
    }

    @Override
    public MethodMapping createMethodMapping(final ClassMapping parent, final MethodSignature signature, final String deobfuscatedName) {
        return new MethodMappingImpl(parent, signature, deobfuscatedName);
    }

    @Override
    public MethodParameterMapping createMethodParameterMapping(final MethodMapping parent, final int index, final String deobfuscatedName) {
        return new MethodParameterMappingImpl(parent, index, deobfuscatedName);
    }

}
