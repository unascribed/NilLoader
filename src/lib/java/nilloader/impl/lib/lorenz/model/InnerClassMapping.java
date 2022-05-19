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

import nilloader.impl.lib.lorenz.merge.MappingSetMerger;

/**
 * Represents a de-obfuscation mapping for an inner class.
 *
 * @author Jamie Mansfield
 * @since 0.1.0
 */
public interface InnerClassMapping extends ClassMapping<InnerClassMapping, ClassMapping>, MemberMapping<InnerClassMapping, ClassMapping> {

    /**
     * Sets the de-obfuscated name of this inner class mapping.
     *
     * <em>Implementations will need to support the input being
     * a fully-qualified name!</em>
     *
     * @param deobfuscatedName The new de-obfuscated name
     * @return {@code this}, for chaining
     */
    @Override
    InnerClassMapping setDeobfuscatedName(final String deobfuscatedName);

    /**
     * {@inheritDoc}
     *
     * <p><strong>Note:</strong> The simple name is empty for anonymous classes.
     * For local classes, the leading digits are stripped.</p>
     */
    @Override
    String getSimpleObfuscatedName();

    /**
     * {@inheritDoc}
     *
     * <p><strong>Note:</strong> The simple name is empty for anonymous classes.
     * For local classes, the leading digits are stripped.</p>
     */
    @Override
    String getSimpleDeobfuscatedName();

    @Override
    default String getFullObfuscatedName() {
        return String.format("%s$%s", this.getParent().getFullObfuscatedName(), this.getObfuscatedName());
    }

    @Override
    default String getFullDeobfuscatedName() {
        return String.format("%s$%s", this.getParent().getFullDeobfuscatedName(), this.getDeobfuscatedName());
    }

    @Override
    default String getObfuscatedPackage() {
        return this.getParent().getObfuscatedPackage();
    }

    @Override
    default String getDeobfuscatedPackage() {
        return this.getParent().getDeobfuscatedPackage();
    }

    @Override
    default InnerClassMapping reverse(final ClassMapping parent) {
        final InnerClassMapping mapping = parent.createInnerClassMapping(this.getDeobfuscatedName(), this.getObfuscatedName());
        this.getFieldMappings().forEach(field -> field.reverse(mapping));
        this.getMethodMappings().forEach(method -> method.reverse(mapping));
        this.getInnerClassMappings().forEach(klass -> klass.reverse(mapping));
        return mapping;
    }

    @Override
    default InnerClassMapping merge(final InnerClassMapping with, final ClassMapping parent) {
        return MappingSetMerger.create(this.getMappings(), with.getMappings()).mergeInnerClass(this, with, parent);
    }

    @Override
    default InnerClassMapping copy(final ClassMapping parent) {
        final InnerClassMapping mapping = parent.createInnerClassMapping(this.getObfuscatedName(), this.getDeobfuscatedName());
        this.getFieldMappings().forEach(field -> field.copy(mapping));
        this.getMethodMappings().forEach(method -> method.copy(mapping));
        this.getInnerClassMappings().forEach(klass -> klass.copy(mapping));
        return mapping;
    }
}
