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
import java.util.jar.Manifest;

/**
 * Represents the manifest entry within a jar file.
 *
 * @author Jamie Mansfield
 * @since 0.3.0
 */
public class JarManifestEntry extends AbstractJarEntry {

    private static final String NAME = "META-INF/MANIFEST.MF";
    private static final String EXTENSION = "MF";

    private final Manifest manifest;

    public JarManifestEntry(final long time, final Manifest manifest) {
        super(NAME, time);
        this.manifest = manifest;
    }

    /**
     * Gets the manifest.
     *
     * @return The manifest
     */
    public final Manifest getManifest() {
        return this.manifest;
    }

    @Override
    public final String getExtension() {
        return EXTENSION;
    }

    @Override
    public final byte[] getContents() {
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            this.manifest.write(baos);
            return baos.toByteArray();
        }
        catch (final IOException ignored) {
            return null;
        }
    }

    @Override
    public JarManifestEntry accept(final JarEntryTransformer vistor) {
        return vistor.transform(this);
    }

}
