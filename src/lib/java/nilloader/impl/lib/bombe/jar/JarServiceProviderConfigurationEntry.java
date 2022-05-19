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

/**
 * Represents a service provider configuration entry
 * within a jar.
 *
 * @author Jamie Mansfield
 * @since 0.3.0
 */
public class JarServiceProviderConfigurationEntry extends AbstractJarEntry {

    private final ServiceProviderConfiguration config;
    private String extension;

    public JarServiceProviderConfigurationEntry(final long time, final ServiceProviderConfiguration config) {
        super("META-INF/services/" + config.getService(), time);
        this.config = config;
    }

    /**
     * Gets the service provider configuration.
     *
     * @return The config
     */
    public final ServiceProviderConfiguration getConfig() {
        return this.config;
    }

    @Override
    public final String getExtension() {
        if (this.extension != null) return this.extension;
        final int index = this.name.lastIndexOf('.');
        if (index == -1) return this.extension = "";
        return this.extension = this.name.substring(index + 1);
    }

    @Override
    public final byte[] getContents() {
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            this.config.write(baos);
            return baos.toByteArray();
        }
        catch (final IOException ignored) {
            return null;
        }
    }

    @Override
    public final JarServiceProviderConfigurationEntry accept(final JarEntryTransformer vistor) {
        return vistor.transform(this);
    }

}
