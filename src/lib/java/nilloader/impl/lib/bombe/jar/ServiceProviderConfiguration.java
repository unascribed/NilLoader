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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A representation of a service provider configuration.
 *
 * @author Jamie Mansfield
 * @since 0.3.0
 */
public class ServiceProviderConfiguration {

    private final String service;
    private final List<String> providers;

    public ServiceProviderConfiguration(final String service, final List<String> providers) {
        this.service = service;
        this.providers = providers;
    }

    public ServiceProviderConfiguration(final String service) {
        this(service, new ArrayList<>());
    }

    /**
     * Gets the name of the service class.
     *
     * @return The service class
     */
    public final String getService() {
        return this.service;
    }

    /**
     * Gets an immutable-view of the provider classes.
     *
     * @return The provider classes
     */
    public final List<String> getProviders() {
        return Collections.unmodifiableList(this.providers);
    }

    /**
     * Adds the given provider to the service configuration.
     *
     * @param provider The provider
     */
    public final void addProvider(final String provider) {
        this.providers.add(provider);
    }

    /**
     * Reads a service provider configuration from the given
     * {@link InputStream}.
     *
     * @param is The input stream
     * @throws IOException If an I/O error occurs
     */
    public void read(final InputStream is) throws IOException {
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            reader.lines()
                    .map(provider -> {
                        final int commentStart = provider.indexOf('#');
                        if (commentStart == -1) return provider;
                        return provider.substring(0, commentStart);
                    })
                    .map(String::trim)
                    .filter(provider -> !provider.isEmpty())
                    .forEach(this.providers::add);
        }
    }

    /**
     * Writes the service provider configuration to the given
     * {@link OutputStream}.
     *
     * @param os The output stream
     * @throws IOException If an I/O error occurs
     */
    public void write(final OutputStream os) throws IOException {
        try (final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os))) {
            for (final String provider : this.providers) {
                writer.write(provider);
                writer.newLine();
            }
        }
    }

}
