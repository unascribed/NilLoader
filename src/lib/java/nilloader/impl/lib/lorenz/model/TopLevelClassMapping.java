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

package nilloader.impl.lib.lorenz.model;

import nilloader.impl.lib.lorenz.MappingSet;
import nilloader.impl.lib.lorenz.merge.MappingSetMerger;

/**
 * Represents a de-obfuscation mapping for a top-level class.
 *
 * @author Jamie Mansfield
 * @since 0.1.0
 */
public interface TopLevelClassMapping extends ClassMapping<TopLevelClassMapping, MappingSet> {

    @Override
    default String getSimpleObfuscatedName() {
        final String name = this.getObfuscatedName();
        final int classIndex = name.lastIndexOf('/');
        return classIndex >= 0 ? name.substring(classIndex + 1) : name;
    }

    @Override
    default String getSimpleDeobfuscatedName() {
        final String name = this.getDeobfuscatedName();
        final int classIndex = name.lastIndexOf('/');
        return classIndex >= 0 ? name.substring(classIndex + 1) : name;
    }

    @Override
    default String getFullObfuscatedName() {
        return this.getObfuscatedName();
    }

    @Override
    default String getFullDeobfuscatedName() {
        return this.getDeobfuscatedName();
    }

    @Override
    default String getObfuscatedPackage() {
        final String name = this.getObfuscatedName();
        final int classIndex = name.lastIndexOf('/');
        return classIndex >= 0 ? name.substring(0, classIndex) : "";
    }

    @Override
    default String getDeobfuscatedPackage() {
        final String name = this.getDeobfuscatedName();
        final int classIndex = name.lastIndexOf('/');
        return classIndex >= 0 ? name.substring(0, classIndex) : "";
    }

    @Override
    default TopLevelClassMapping reverse(final MappingSet parent) {
        final TopLevelClassMapping mapping = parent.createTopLevelClassMapping(this.getDeobfuscatedName(), this.getObfuscatedName());
        this.getFieldMappings().forEach(field -> field.reverse(mapping));
        this.getMethodMappings().forEach(method -> method.reverse(mapping));
        this.getInnerClassMappings().forEach(klass -> klass.reverse(mapping));
        return mapping;
    }

    @Override
    default TopLevelClassMapping merge(final TopLevelClassMapping with, final MappingSet parent) {
        return MappingSetMerger.create(this.getMappings(), with.getMappings()).mergeTopLevelClass(this, with, parent);
    }

    @Override
    default TopLevelClassMapping copy(final MappingSet parent) {
        final TopLevelClassMapping mapping = parent.createTopLevelClassMapping(this.getObfuscatedName(), this.getDeobfuscatedName());
        this.getFieldMappings().forEach(field -> field.copy(mapping));
        this.getMethodMappings().forEach(method -> method.copy(mapping));
        this.getInnerClassMappings().forEach(klass -> klass.copy(mapping));
        return mapping;
    }

}
