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

package nilloader.impl.lib.lorenz.io.srg.tsrg;

import java.io.Writer;

import nilloader.impl.lib.lorenz.MappingSet;
import nilloader.impl.lib.lorenz.io.MappingsWriter;
import nilloader.impl.lib.lorenz.io.TextMappingsWriter;
import nilloader.impl.lib.lorenz.model.ClassMapping;
import nilloader.impl.lib.lorenz.model.FieldMapping;
import nilloader.impl.lib.lorenz.model.Mapping;
import nilloader.impl.lib.lorenz.model.MethodMapping;

/**
 * An implementation of {@link MappingsWriter} for the TSRG format.
 *
 * @author Jamie Mansfield
 * @since 0.4.0
 */
public class TSrgWriter extends TextMappingsWriter {

    /**
     * Creates a new TSRG mappings writer, from the given {@link Writer}.
     *
     * @param writer The writer
     */
    public TSrgWriter(final Writer writer) {
        super(writer);
    }

    @Override
    public void write(final MappingSet mappings) {
        // Write class mappings
        mappings.getTopLevelClassMappings().stream()
                .filter(ClassMapping::hasMappings)
                .sorted(this.getConfig().getClassMappingComparator())
                .forEach(this::writeClassMapping);
    }

    /**
     * Writes the given {@link ClassMapping}, alongside its member mappings.
     *
     * @param mapping The class mapping
     */
    protected void writeClassMapping(final ClassMapping<?, ?> mapping) {
        // Effectively ClassMapping#hasMappings() without the inner class check
        if (mapping.hasDeobfuscatedName() ||
                mapping.getFieldsByName().values().stream().anyMatch(Mapping::hasDeobfuscatedName) ||
                mapping.getMethodMappings().stream().anyMatch(MethodMapping::hasMappings)) {
            this.writer.println(String.format("%s %s", mapping.getFullObfuscatedName(), mapping.getFullDeobfuscatedName()));
        }

        // Write field mappings
        mapping.getFieldsByName().values().stream()
                .filter(Mapping::hasDeobfuscatedName)
                .sorted(this.getConfig().getFieldMappingComparator())
                .forEach(this::writeFieldMapping);

        // Write method mappings
        mapping.getMethodMappings().stream()
                .filter(Mapping::hasDeobfuscatedName)
                .sorted(this.getConfig().getMethodMappingComparator())
                .forEach(this::writeMethodMapping);

        // Write inner class mappings
        mapping.getInnerClassMappings().stream()
                .filter(ClassMapping::hasMappings)
                .sorted(this.getConfig().getClassMappingComparator())
                .forEach(this::writeClassMapping);
    }

    /**
     * Writes the given {@link FieldMapping}.
     *
     * @param mapping The field mapping
     */
    protected void writeFieldMapping(final FieldMapping mapping) {
        // The SHOULD_WRITE test should have already have been performed, so we're good
        this.writer.println(String.format("\t%s %s", mapping.getObfuscatedName(), mapping.getDeobfuscatedName()));
    }

    /**
     * Writes the given {@link MethodMapping}.
     *
     * @param mapping The method mapping
     */
    protected void writeMethodMapping(final MethodMapping mapping) {
        // The SHOULD_WRITE test should have already have been performed, so we're good
        this.writer.println(String.format("\t%s %s %s",
                mapping.getObfuscatedName(), mapping.getObfuscatedDescriptor(),
                mapping.getDeobfuscatedName()));
    }

}
