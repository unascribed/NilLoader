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

package nilloader.impl.lib.bombe.jar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import nilloader.impl.lib.bombe.util.ByteStreams;

/**
 * Utilities for working with jar files.
 *
 * @author Jamie Mansfield
 * @since 0.3.0
 */
public final class Jars {

    /**
     * Walks through the entries within the given {@link JarFile}.
     *
     * @param jarFile The jar file
     * @return The jar entries
     */
    public static Stream<AbstractJarEntry> walk(final JarFile jarFile) {
        return jarFile.stream().filter(entry -> !entry.isDirectory()).map(entry -> {
            final String name = entry.getName();
            try (final InputStream stream = jarFile.getInputStream(entry)) {
                final long time = entry.getTime();

                if (Objects.equals("META-INF/MANIFEST.MF", entry.getName())) {
                    return new JarManifestEntry(time, new Manifest(stream));
                }
                else if (entry.getName().startsWith("META-INF/services/")) {
                    final String serviceName = entry.getName().substring("META-INF/services/".length());

                    final ServiceProviderConfiguration config = new ServiceProviderConfiguration(serviceName);
                    config.read(stream);
                    return new JarServiceProviderConfigurationEntry(time, config);
                }

                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ByteStreams.copy(stream, baos);

                if (entry.getName().endsWith(".class")) {
                    return new JarClassEntry(name, time, baos.toByteArray());
                }
                else {
                    return new JarResourceEntry(name, time, baos.toByteArray());
                }
            }
            catch (final IOException ignored) {
                // TODO: handle?
                return null;
            }
        });
    }

    /**
     * Transforms the entries within the given input {@link Path} with the transformers,
     * to the given output {@link Path}.
     *
     * @param input The input jar path
     * @param output The output jar path
     * @param transformers The transformers
     * @throws IOException If an I/O exception occurs
     */
    public static void transform(final Path input, final Path output, final JarEntryTransformer... transformers) throws IOException {
        try (final JarFile jarFile = new JarFile(input.toFile());
             final JarOutputStream jos = new JarOutputStream(Files.newOutputStream(output))) {
            transform(jarFile, jos, transformers);
        }
    }

    /**
     * Transforms the entries with the given {@link JarFile} with the transformers,
     * to the given {@link JarOutputStream}.
     *
     * @param jarFile The jar file
     * @param jos The jar output stream
     * @param transformers The transformers to apply
     * @return The jar output stream
     */
    public static JarOutputStream transform(final JarFile jarFile, final JarOutputStream jos, final JarEntryTransformer... transformers) {
        final Set<String> packages = new HashSet<>();
        walk(jarFile)
                .map(entry -> {
                    for (final JarEntryTransformer transformer : transformers) {
                        entry = entry.accept(transformer);
                    }
                    return entry;
                })
                .forEach(entry -> {
                    try {
                        if (!packages.contains(entry.getPackage())) {
                            packages.add(entry.getPackage());
                            jos.putNextEntry(new JarEntry(entry.getPackage() + "/"));
                            jos.closeEntry();
                        }

                        entry.write(jos);
                    }
                    catch (final IOException ex) {
                        ex.printStackTrace();
                        // todo:
                    }
                });

        return jos;
    }

    private Jars() {
    }

}
