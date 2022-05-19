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

import nilloader.api.lib.asm.ClassReader;
import nilloader.api.lib.asm.ClassVisitor;
import nilloader.api.lib.asm.ClassWriter;
import nilloader.api.lib.asm.commons.ClassRemapper;
import nilloader.api.lib.asm.commons.Remapper;
import nilloader.impl.lib.bombe.jar.JarClassEntry;
import nilloader.impl.lib.bombe.jar.JarEntryTransformer;
import nilloader.impl.lib.bombe.jar.JarManifestEntry;
import nilloader.impl.lib.bombe.jar.JarServiceProviderConfigurationEntry;
import nilloader.impl.lib.bombe.jar.ServiceProviderConfiguration;

import java.util.List;
import java.util.function.BiFunction;
import java.util.jar.Attributes;
import java.util.stream.Collectors;

/**
 * An implementation of {@link JarEntryTransformer} for remapping classes
 * using a {@link Remapper}.
 *
 * @author Jamie Mansfield
 * @since 0.3.0
 */
public class JarEntryRemappingTransformer implements JarEntryTransformer {

    private final Remapper remapper;
    private final BiFunction<ClassVisitor, Remapper, ClassRemapper> clsRemapper;

    public JarEntryRemappingTransformer(final Remapper remapper, final BiFunction<ClassVisitor, Remapper, ClassRemapper> clsRemapper) {
        this.remapper = remapper;
        this.clsRemapper = clsRemapper;
    }

    public JarEntryRemappingTransformer(final Remapper remapper) {
        this(remapper, ClassRemapper::new);
    }

    @Override
    public JarClassEntry transform(final JarClassEntry entry) {
        // Remap the class
        final ClassReader reader = new ClassReader(entry.getContents());
        final ClassWriter writer = new ClassWriter(reader, 0);
        reader.accept(this.clsRemapper.apply(
                writer,
                this.remapper
        ), 0);

        // Create the jar entry
        final String originalName = entry.getName().substring(0, entry.getName().length() - ".class".length());
        final String name = this.remapper.map(originalName) + ".class";
        return new JarClassEntry(name, entry.getTime(), writer.toByteArray());
    }

    @Override
    public JarManifestEntry transform(final JarManifestEntry entry) {
        // Remap the Main-Class attribute, if present
        if (entry.getManifest().getMainAttributes().containsKey(new Attributes.Name("Main-Class"))) {
            final String mainClassObf = entry.getManifest().getMainAttributes().getValue("Main-Class")
                    .replace('.', '/');
            final String mainClassDeobf = this.remapper.map(mainClassObf)
                    .replace('/', '.');

            // Since Manifest is mutable, we need'nt create a new entry \o/
            entry.getManifest().getMainAttributes().putValue("Main-Class", mainClassDeobf);
        }

        return entry;
    }

    @Override
    public JarServiceProviderConfigurationEntry transform(final JarServiceProviderConfigurationEntry entry) {
        // Remap the Service class
        final String obfServiceName = entry.getConfig().getService()
                .replace('.', '/');
        final String deobfServiceName = this.remapper.map(obfServiceName)
                .replace('/', '.');

        // Remap the Provider classes
        final List<String> deobfProviders = entry.getConfig().getProviders().stream()
                .map(provider -> provider.replace('.', '/'))
                .map(this.remapper::map)
                .map(provider -> provider.replace('/', '.'))
                .collect(Collectors.toList());

        // Create the new entry
        final ServiceProviderConfiguration config = new ServiceProviderConfiguration(deobfServiceName, deobfProviders);
        return new JarServiceProviderConfigurationEntry(entry.getTime(), config);
    }

}
