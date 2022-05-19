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

package nilloader.impl.lib.lorenz.model.jar;

import nilloader.impl.lib.bombe.type.FieldType;
import nilloader.impl.lib.lorenz.model.FieldMapping;

import java.util.Optional;

/**
 * A provider for establishing the type of a field, when it
 * is not already known.
 *
 * This is required as not all de-obfuscation mapping formats
 * include this data. <em>Writers that do need to make use of
 * field types should throw a {@link RuntimeException} if a
 * provider is not present!</em>
 *
 * @author Jamie Mansfield
 * @since 0.3.0
 */
@FunctionalInterface
public interface FieldTypeProvider {

    /**
     * Provides a {@link FieldType} for the given {@link FieldMapping},
     * if possible.
     *
     * @param mapping The field mapping
     * @return The type for the field, wrapped in an {@link Optional}
     * @since 0.4.0
     */
    Optional<FieldType> provide(final FieldMapping mapping);

}
