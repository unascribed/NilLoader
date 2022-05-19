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

package nilloader.impl.lib.lorenz;

import nilloader.impl.lib.bombe.type.signature.FieldSignature;
import nilloader.impl.lib.bombe.type.signature.MethodSignature;
import nilloader.impl.lib.lorenz.model.ClassMapping;
import nilloader.impl.lib.lorenz.model.FieldMapping;
import nilloader.impl.lib.lorenz.model.InnerClassMapping;
import nilloader.impl.lib.lorenz.model.MethodMapping;
import nilloader.impl.lib.lorenz.model.MethodParameterMapping;
import nilloader.impl.lib.lorenz.model.TopLevelClassMapping;

/**
 * A factory intended to facilitate the creating of the many models in Lorenz.
 *
 * <p>The intention of this factory is to allow the Lorenz implementation to be
 * mixed with custom implementation, allowing developers to avoid having to write
 * lots of standard code.</p>
 *
 * @author Jamie Mansfield
 * @since 0.3.0
 */
public interface MappingSetModelFactory {

    /**
     * Creates a {@link TopLevelClassMapping} linked to the given {@link MappingSet}.
     *
     * @param parent The mapping set to link to
     * @param obfuscatedName The obfuscated name of the class
     * @param deobfuscatedName The de-obfuscated name to give the class
     * @return The class mapping
     */
    TopLevelClassMapping createTopLevelClassMapping(final MappingSet parent, final String obfuscatedName, final String deobfuscatedName);

    /**
     * Creates a {@link InnerClassMapping} linked to the given {@link ClassMapping}.
     *
     * @param parent The class mapping to link to
     * @param obfuscatedName The obfuscated name of the class
     * @param deobfuscatedName The de-obfuscated name to give the class
     * @return The class mapping
     */
    InnerClassMapping createInnerClassMapping(final ClassMapping parent, final String obfuscatedName, final String deobfuscatedName);

    /**
     * Creates a {@link FieldMapping} linked to the given {@link ClassMapping}.
     *
     * @param parent The class mapping to link to
     * @param signature The signature of the field
     * @param deobfuscatedName The de-obfuscated name to give the field
     * @return The field mapping
     * @since 0.4.0
     */
    FieldMapping createFieldMapping(final ClassMapping parent, final FieldSignature signature, final String deobfuscatedName);

    /**
     * Creates a {@link MethodMapping} linked to the given {@link ClassMapping}.
     *
     * @param parent The class mapping to link to
     * @param signature The signature of the obfuscated method
     * @param deobfuscatedName The de-obfuscated name to give the method
     * @return The method mapping
     */
    MethodMapping createMethodMapping(final ClassMapping parent, final MethodSignature signature, final String deobfuscatedName);

    /**
     * Creates a {@link MethodParameterMapping} linked to the given {@link MethodMapping}.
     *
     * @param parent The method mapping to link to
     * @param index The index of the parameter
     * @param deobfuscatedName The de-obfuscated name to give the parameter
     * @return The method parameter mapping
     * @since 0.4.0
     */
    MethodParameterMapping createMethodParameterMapping(final MethodMapping parent, final int index, final String deobfuscatedName);

}
