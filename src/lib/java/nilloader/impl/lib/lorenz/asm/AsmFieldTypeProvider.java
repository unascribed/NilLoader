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

import nilloader.api.lib.asm.tree.ClassNode;
import nilloader.api.lib.asm.tree.FieldNode;
import nilloader.impl.lib.bombe.asm.jar.ClassProvider;
import nilloader.impl.lib.bombe.type.FieldType;
import nilloader.impl.lib.lorenz.model.FieldMapping;
import nilloader.impl.lib.lorenz.model.jar.FieldTypeProvider;

import java.util.Objects;
import java.util.Optional;

/**
 * An implementation of {@link FieldTypeProvider} backed by a
 * {@link ClassProvider}.
 *
 * @author Jamie Mansfield
 * @since 0.4.0
 */
public class AsmFieldTypeProvider implements FieldTypeProvider {

    private final ClassProvider classProvider;

    public AsmFieldTypeProvider(final ClassProvider classProvider) {
        this.classProvider = classProvider;
    }

    @Override
    public Optional<FieldType> provide(final FieldMapping mapping) {
        final String owner = mapping.getParent().getFullObfuscatedName();

        final ClassNode node = this.classProvider.getAsNode(owner);
        if (node == null) return Optional.empty();

        final Optional<FieldNode> fieldNode = node.fields.stream()
                .filter(field -> Objects.equals(field.name, mapping.getObfuscatedName()))
                .findAny();
        if (fieldNode.isPresent()) {
            final FieldType type = FieldType.of(fieldNode.get().desc);
            return Optional.of(type);
        }

        return Optional.empty();
    }

}
