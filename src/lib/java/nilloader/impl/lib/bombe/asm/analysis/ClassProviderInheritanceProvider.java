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

package nilloader.impl.lib.bombe.asm.analysis;

import nilloader.api.lib.asm.ClassReader;
import nilloader.api.lib.asm.Opcodes;
import nilloader.impl.lib.bombe.analysis.InheritanceProvider;
import nilloader.impl.lib.bombe.asm.jar.ClassProvider;

import java.util.Optional;

/**
 * An implementation of {@link InheritanceProvider} that retrieves all of
 * its information from a {@link ClassProvider}.
 *
 * @author Jamie Mansfield
 * @since 0.3.0
 */
public class ClassProviderInheritanceProvider implements InheritanceProvider {

    private final int api;
    private final ClassProvider provider;

    /**
     * Creates a new inheritance provider backed by a class provider.
     *
     * @param api The ASM API version to use
     * @param provider The class provider
     * @since 0.3.3
     */
    public ClassProviderInheritanceProvider(final int api, final ClassProvider provider) {
        this.api = api;
        this.provider = provider;
    }

    /**
     * Creates a new inheritance provider backed by a class provider, defaulting to
     * {@link Opcodes#ASM7}.
     *
     * @param provider The class provider
     */
    public ClassProviderInheritanceProvider(final ClassProvider provider) {
        this(Opcodes.ASM7, provider);
    }

    @Override
    public Optional<ClassInfo> provide(final String klass) {
        final byte[] classBytes = this.provider.get(klass);
        if (classBytes == null) return Optional.empty();

        final ClassReader reader = new ClassReader(classBytes);
        final InheritanceClassInfoVisitor classInfoVisitor = new InheritanceClassInfoVisitor(this.api);
        reader.accept(classInfoVisitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return Optional.of(classInfoVisitor.create());
    }

}
