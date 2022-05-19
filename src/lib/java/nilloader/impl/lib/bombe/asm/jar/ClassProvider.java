/*
 * Copyright (c) 2018, Jamie Mansfield <https://jamiemansfield.me/>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *  Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package nilloader.impl.lib.bombe.asm.jar;

import java.util.jar.JarFile;

import nilloader.api.lib.asm.ClassReader;
import nilloader.api.lib.asm.tree.ClassNode;

/**
 * A provider of classes.
 *
 * @author Jamie Mansfield
 * @since 0.3.0
 */
@FunctionalInterface
public interface ClassProvider {

    /**
     * Creates a class provider for the given {@link ClassLoader}.
     *
     * @param loader The class loader
     * @return The class provider
     */
    static ClassProvider of(final ClassLoader loader) {
        return new ClassLoaderClassProvider(loader);
    }

    /**
     * Creates a class provider for the given {@link JarFile}.
     *
     * @param jar The jar file
     * @return The class provider
     */
    static ClassProvider of(final JarFile jar) {
        return new JarFileClassProvider(jar);
    }

    /**
     * Gets the given class, represented as a byte array.
     *
     * @param klass The name of the class
     * @return The class, or {@code null} if unavailable
     */
    byte[] get(final String klass);

    /**
     * Gets the given class, represented as a {@link ClassNode}.
     *
     * @param klass The name of the class
     * @param parsingOptions The parsing options
     * @return The class node, or {@code null} if unavailable
     */
    default ClassNode getAsNode(final String klass, final int parsingOptions) {
        final byte[] contents = this.get(klass);
        if (contents == null) return null;

        final ClassReader reader = new ClassReader(contents);
        final ClassNode node = new ClassNode();
        reader.accept(node, parsingOptions);
        return node;
    }

    /**
     * Gets the given class, represented as a {@link ClassNode}.
     *
     * @param klass The name of the class
     * @return The class node
     */
    default ClassNode getAsNode(final String klass) {
        return this.getAsNode(klass, 0);
    }

}
