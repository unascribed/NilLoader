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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import nilloader.impl.lib.lorenz.MappingSet;
import nilloader.impl.lib.lorenz.MappingSetModelFactory;
import nilloader.impl.lib.lorenz.model.TopLevelClassMapping;
import nilloader.impl.lib.lorenz.model.jar.CascadingFieldTypeProvider;

/**
 * A basic implementation of {@link MappingSet}.
 *
 * @author Jamie Mansfield
 * @since 0.2.0
 */
public class MappingSetImpl implements MappingSet {

    private final Map<String, TopLevelClassMapping> topLevelClasses = new ConcurrentHashMap<>();
    private final MappingSetModelFactory modelFactory;
    private CascadingFieldTypeProvider fieldTypeProvider = new CascadingFieldTypeProvider();

    public MappingSetImpl() {
        this(new MappingSetModelFactoryImpl());
    }

    public MappingSetImpl(final MappingSetModelFactory modelFactory) {
        this.modelFactory = modelFactory;
    }

    @Override
    public MappingSetModelFactory getModelFactory() {
        return this.modelFactory;
    }

    @Override
    public Collection<TopLevelClassMapping> getTopLevelClassMappings() {
        return Collections.unmodifiableCollection(this.topLevelClasses.values());
    }

    @Override
    public TopLevelClassMapping createTopLevelClassMapping(final String obfuscatedName, final String deobfuscatedName) {
        return this.topLevelClasses.compute(obfuscatedName.replace('.', '/'), (name, existingMapping) -> {
            if (existingMapping != null) return existingMapping.setDeobfuscatedName(deobfuscatedName);
            return this.getModelFactory().createTopLevelClassMapping(this, name, deobfuscatedName);
        });
    }

    @Override
    public Optional<TopLevelClassMapping> getTopLevelClassMapping(final String obfuscatedName) {
        return Optional.ofNullable(this.topLevelClasses.get(obfuscatedName.replace('.', '/')));
    }

    @Override
    public boolean hasTopLevelClassMapping(final String obfuscatedName) {
        return this.topLevelClasses.containsKey(obfuscatedName.replace('.', '/'));
    }

    @Override
    public CascadingFieldTypeProvider getFieldTypeProvider() {
        return this.fieldTypeProvider;
    }

}
