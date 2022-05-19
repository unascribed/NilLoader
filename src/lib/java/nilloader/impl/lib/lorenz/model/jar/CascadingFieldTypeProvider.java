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

package nilloader.impl.lib.lorenz.model.jar;

import nilloader.impl.lib.bombe.type.FieldType;
import nilloader.impl.lib.lorenz.model.FieldMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * An implementation of {@link FieldTypeProvider} that is backed by
 * many - allowing multiples sources to be used.
 *
 * @author Jamie Mansfield
 * @since 0.4.0
 */
public class CascadingFieldTypeProvider implements FieldTypeProvider {

    private final List<FieldTypeProvider> providers = new ArrayList<>();

    /**
     * Adds a {@link FieldTypeProvider} to the provider.
     *
     * @param provider The provider
     * @return {@code this}, for chaining
     */
    public CascadingFieldTypeProvider add(final FieldTypeProvider provider) {
        this.providers.add(provider);
        return this;
    }

    /**
     * Removes a {@link FieldTypeProvider} from the provider.
     *
     * @param provider The provider
     * @return {@code this}, for chaining
     */
    public CascadingFieldTypeProvider remove(final FieldTypeProvider provider) {
        this.providers.remove(provider);
        return this;
    }

    @Override
    public Optional<FieldType> provide(final FieldMapping mapping) {
        for (final FieldTypeProvider provider : this.providers) {
            final Optional<FieldType> type = provider.provide(mapping);
            if (type.isPresent()) return type;
        }
        return Optional.empty();
    }

}
