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

package nilloader.impl.lib.bombe.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A cascading {@link InheritanceProvider} allows for class information to be
 * pooled from multiple sources.
 *
 * @author Jamie Mansfield
 * @since 0.1.0
 */
public class CascadingInheritanceProvider implements InheritanceProvider {

    private final ArrayList<InheritanceProvider> providers;

    public CascadingInheritanceProvider(final List<InheritanceProvider> providers) {
        this.providers = new ArrayList<>(providers);
    }

    public CascadingInheritanceProvider() {
        this.providers = new ArrayList<>();
    }

    /**
     * Adds an {@link InheritanceProvider} that can be used for obtaining class
     * information.
     *
     * @param provider The inheritance provider
     * @return {@code this}, for chaining
     */
    public CascadingInheritanceProvider install(final InheritanceProvider provider) {
        this.providers.add(provider);
        return this;
    }

    @Override
    public Optional<ClassInfo> provide(final String klass) {
        for (final InheritanceProvider provider : this.providers) {
            final Optional<ClassInfo> info = provider.provide(klass);
            if (info.isPresent()) return info;
        }
        return Optional.empty();
    }

    @Override
    public Optional<ClassInfo> provide(String klass, Object context) {
        for (final InheritanceProvider provider : this.providers) {
            final Optional<ClassInfo> info = provider.provide(klass, context);
            if (info.isPresent()) return info;
        }
        return Optional.empty();
    }

}
