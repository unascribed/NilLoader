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

import nilloader.impl.lib.lorenz.model.ClassMapping;
import nilloader.impl.lib.lorenz.model.InnerClassMapping;

/**
 * A basic implementation of {@link InnerClassMapping}.
 *
 * @author Jamie Mansfield
 * @since 0.2.0
 */
public class InnerClassMappingImpl extends AbstractClassMappingImpl<InnerClassMapping, ClassMapping> implements InnerClassMapping {

    private final ClassMapping parentClass;

    /**
     * Creates a new inner class mapping, from the given parameters.
     *
     * @param parentClass The class mapping, this mapping belongs to
     * @param obfuscatedName The obfuscated name
     * @param deobfuscatedName The de-obfuscated name
     */
    public InnerClassMappingImpl(final ClassMapping parentClass, final String obfuscatedName, final String deobfuscatedName) {
        super(parentClass.getMappings(), obfuscatedName, deobfuscatedName);
        this.parentClass = parentClass;
    }

    @Override
    public ClassMapping getParent() {
        return this.parentClass;
    }

    @Override
    public String getSimpleObfuscatedName() {
        return stripAsciiDigits(this.getObfuscatedName());
    }

    @Override
    public String getSimpleDeobfuscatedName() {
        return stripAsciiDigits(this.getDeobfuscatedName());
    }

    @Override
    public boolean hasDeobfuscatedName() {
        // If a parent class has a deobfuscated name, then we do too, since we inherit it
        return this.getParent().hasDeobfuscatedName() || super.hasDeobfuscatedName();
    }

    private static String stripAsciiDigits(final String name) {
        for (int pos = 0; pos < name.length(); pos++) {
            if (!isAsciiDigit(name.charAt(pos))) {
                return name.substring(pos);
            }
        }
        return "";
    }

    private static boolean isAsciiDigit(final char c) {
        return '0' <= c && c <= '9';
    }

    @Override
    public InnerClassMapping setDeobfuscatedName(final String deobfuscatedName) {
        final int lastIndex = deobfuscatedName.lastIndexOf('$');
        if (lastIndex == -1) {
            return super.setDeobfuscatedName(deobfuscatedName);
        }

        // Split the obfuscated name, to fetch the parent class name, and inner class name
        final String innerClassName = deobfuscatedName.substring(lastIndex + 1);

        // Set the correct class name!
        return super.setDeobfuscatedName(innerClassName);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (!(obj instanceof InnerClassMapping)) return false;

        final InnerClassMapping that = (InnerClassMapping) obj;
        return Objects.equals(this.parentClass, that.getParent());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.parentClass.getFullObfuscatedName(), this.parentClass.getFullDeobfuscatedName());
    }

}
