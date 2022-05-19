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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * An {@link InheritanceProvider} that wraps another {@link InheritanceProvider}
 * and caches all requests. If information is needed more than once, use of this
 * class is recommended to improve performance.
 *
 * @author Minecrell
 * @since 0.3.0
 */
public class CachingInheritanceProvider implements InheritanceProvider {

    private final InheritanceProvider provider;
    private final Map<String, Optional<ClassInfo>> cache = new HashMap<>();

    public CachingInheritanceProvider(final InheritanceProvider provider) {
        this.provider = provider;
    }

    @Override
    public Optional<ClassInfo> provide(final String klass) {
        return this.cache.computeIfAbsent(klass, this.provider::provide);
    }

    @Override
    public Optional<ClassInfo> provide(final String klass, final Object context) {
        return this.cache.computeIfAbsent(klass, k -> this.provider.provide(k, context));
    }

}
