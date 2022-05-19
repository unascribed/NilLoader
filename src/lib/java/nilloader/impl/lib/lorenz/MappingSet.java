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

package nilloader.impl.lib.lorenz;

import nilloader.impl.lib.bombe.type.ArrayType;
import nilloader.impl.lib.bombe.type.FieldType;
import nilloader.impl.lib.bombe.type.MethodDescriptor;
import nilloader.impl.lib.bombe.type.ObjectType;
import nilloader.impl.lib.bombe.type.Type;
import nilloader.impl.lib.lorenz.impl.MappingSetImpl;
import nilloader.impl.lib.lorenz.merge.MappingSetMerger;
import nilloader.impl.lib.lorenz.model.ClassMapping;
import nilloader.impl.lib.lorenz.model.TopLevelClassMapping;
import nilloader.impl.lib.lorenz.model.jar.CascadingFieldTypeProvider;
import nilloader.impl.lib.lorenz.model.jar.FieldTypeProvider;
import nilloader.impl.lib.lorenz.util.Reversible;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The container for {@link TopLevelClassMapping}s, allowing for their creating and
 * locating any {@link ClassMapping}.
 *
 * @author Jamie Mansfield
 * @since 0.1.0
 */
public interface MappingSet extends Reversible<MappingSet, MappingSet> {

    /**
     * Creates a mapping set, using the default Lorenz model implementation.
     *
     * @return The mapping set
     * @since 0.2.0
     */
    static MappingSet create() {
        return new MappingSetImpl();
    }

    /**
     * Creates a mapping set, using the given model factory.
     *
     * @param modelFactory The model factory to use
     * @return The mapping set
     * @since 0.3.0
     */
    static MappingSet create(final MappingSetModelFactory modelFactory) {
        return new MappingSetImpl(modelFactory);
    }

    /**
     * Gets the underlying model factory, that is used to construct
     * the implementation classes for all the models.
     *
     * @return The model factory
     */
    MappingSetModelFactory getModelFactory();

    /**
     * Gets an immutable collection of all of the top-level class
     * mappings of the mapping set.
     *
     * @return The top-level class mappings
     */
    Collection<TopLevelClassMapping> getTopLevelClassMappings();

    /**
     * Creates a top-level class mapping with the given obfuscated and de-obfuscated
     * names.
     *
     * @param obfuscatedName The obfuscated name of the top-level class
     * @param deobfuscatedName The de-obfuscated name of the top-level class
     * @return The top-level class mapping, to allow for chaining
     */
    TopLevelClassMapping createTopLevelClassMapping(final String obfuscatedName, final String deobfuscatedName);

    /**
     * Gets the top-level class mapping of the given obfuscated name of the
     * class mapping, should it exist.
     *
     * @param obfuscatedName The obfuscated name of the top-level class mapping
     * @return The top-level class mapping, wrapped in an {@link Optional}
     */
    Optional<TopLevelClassMapping> getTopLevelClassMapping(final String obfuscatedName);

    /**
     * Gets, or creates should it not exist, a top-level class mapping of the
     * given obfuscated name.
     *
     * @param obfuscatedName The obfuscated name of the top-level class mapping
     * @return The top-level class mapping
     */
    default TopLevelClassMapping getOrCreateTopLevelClassMapping(final String obfuscatedName) {
        return this.getTopLevelClassMapping(obfuscatedName)
                .orElseGet(() -> this.createTopLevelClassMapping(obfuscatedName, obfuscatedName));
    }

    /**
     * Establishes whether the mapping set contains a top-level class
     * mapping of the given obfuscated name.
     *
     * @param obfuscatedName The obfuscated name of the top-level class
     *                       mapping
     * @return {@code true} should a top-level class mapping of the
     *         given obfuscated name exist in the mapping set;
     *         {@code false} otherwise
     */
    boolean hasTopLevelClassMapping(final String obfuscatedName);

    /**
     * Gets the class mapping of the given obfuscated name.
     *
     * @param obfuscatedName The obfuscated name
     * @return The class mapping, wrapped in an {@link Optional}
     */
    default Optional<? extends ClassMapping<?, ?>> getClassMapping(final String obfuscatedName) {
        final int lastIndex = obfuscatedName.lastIndexOf('$');
        if (lastIndex == -1) return this.getTopLevelClassMapping(obfuscatedName);

        // Split the obfuscated name, to fetch the parent class name, and inner class name
        final String parentClassName = obfuscatedName.substring(0, lastIndex);
        final String innerClassName = obfuscatedName.substring(lastIndex + 1);

        // Get the parent class
        return this.getClassMapping(parentClassName)
                // Get and return the inner class
                .flatMap(parentClassMapping -> parentClassMapping.getInnerClassMapping(innerClassName));
    }

    /**
     * Attempts to locate a class mapping for the given obfuscated name.
     *
     * <p>This is equivalent to calling {@link #getClassMapping(String)},
     * except that it will insert a new inner class mapping in case a
     * class mapping for the outer class exists.</p>
     *
     * <p>This method exists to simplify remapping, where it is important
     * to keep inner classes a part of the outer class.</p>
     *
     * @param obfuscatedName The obfuscated name
     * @return The class mapping, wrapped in an {@link Optional}
     */
    default Optional<? extends ClassMapping<?, ?>> computeClassMapping(final String obfuscatedName) {
        final int lastIndex = obfuscatedName.lastIndexOf('$');
        if (lastIndex == -1) return this.getTopLevelClassMapping(obfuscatedName);

        // Split the obfuscated name, to fetch the parent class name, and inner class name
        final String parentClassName = obfuscatedName.substring(0, lastIndex);
        final String innerClassName = obfuscatedName.substring(lastIndex + 1);

        // Get the parent class
        return this.getClassMapping(parentClassName)
                // Get and return the inner class
                .map(parentClassMapping -> parentClassMapping.getOrCreateInnerClassMapping(innerClassName));
    }

    /**
     * Gets, or creates should it not exist, a class mapping, of the given
     * obfuscated name.
     *
     * @param obfuscatedName The obfuscated name of the class mapping
     * @return The class mapping
     */
    default ClassMapping<?, ?> getOrCreateClassMapping(final String obfuscatedName) {
        final int lastIndex = obfuscatedName.lastIndexOf('$');
        if (lastIndex == -1) return this.getOrCreateTopLevelClassMapping(obfuscatedName);

        // Split the obfuscated name, to fetch the parent class name, and inner class name
        final String parentClassName = obfuscatedName.substring(0, lastIndex);
        final String innerClassName = obfuscatedName.substring(lastIndex + 1);

        // Get the parent class
        final ClassMapping parentClass = this.getOrCreateClassMapping(parentClassName);

        // Get the inner class
        return parentClass.getOrCreateInnerClassMapping(innerClassName);
    }

    /**
     * Gets the field type provider in use for this set of mappings.
     *
     * @return The field type provider
     * @since 0.4.0
     */
    CascadingFieldTypeProvider getFieldTypeProvider();

    /**
     * Adds the given {@link FieldTypeProvider} to this set of mappings.
     *
     * @param fieldTypeProvider The field type provider
     * @return {@code this}, for chaining
     * @see CascadingFieldTypeProvider#add(FieldTypeProvider)
     * @since 0.4.0
     */
    default MappingSet addFieldTypeProvider(final FieldTypeProvider fieldTypeProvider) {
        this.getFieldTypeProvider().add(fieldTypeProvider);
        return this;
    }

    /**
     * Removes the given {@link FieldTypeProvider} to this set of mappings.
     *
     * @param fieldTypeProvider The field type provider
     * @return {@code this}, for chaining
     * @see CascadingFieldTypeProvider#remove(FieldTypeProvider)
     * @since 0.4.0
     */
    default MappingSet removeFieldTypeProvider(final FieldTypeProvider fieldTypeProvider) {
        this.getFieldTypeProvider().remove(fieldTypeProvider);
        return this;
    }

    /**
     * Gets the de-obfuscated view of the given type.
     *
     * @param type The type to de-obfuscate
     * @return The de-obfuscated type
     * @since 0.5.0
     */
    default Type deobfuscate(final Type type) {
        if (type instanceof FieldType) {
            return this.deobfuscate(this.deobfuscate((FieldType) type));
        }
        return type;
    }

    /**
     * Gets the de-obfuscated view of the given field type.
     *
     * @param type The type to de-obfuscate
     * @return The de-obfuscated type
     * @since 0.5.0
     */
    default FieldType deobfuscate(final FieldType type) {
        if (type instanceof ArrayType) {
            final ArrayType arr = (ArrayType) type;
            final FieldType component = this.deobfuscate(arr.getComponent());
            return component == arr.getComponent() ?
                    arr :
                    new ArrayType(arr.getDimCount(), component);
        }
        else if (type instanceof ObjectType) {
            final ObjectType obj = (ObjectType) type;
            return this.getClassMapping(obj.getClassName())
                    .map(m -> new ObjectType(m.getFullDeobfuscatedName()))
                    .orElse(obj);
        }
        return type;
    }

    /**
     * Gets the de-obfuscated descriptor of the method.
     *
     * @param descriptor The descriptor to de-obfuscate
     * @return The de-obfuscated descriptor
     * @since 0.5.0
     */
    default MethodDescriptor deobfuscate(final MethodDescriptor descriptor) {
        return new MethodDescriptor(
                descriptor.getParamTypes().stream()
                        .map(this::deobfuscate)
                        .collect(Collectors.toList()),
                this.deobfuscate(descriptor.getReturnType())
        );
    }

    /**
     * Produces a new mapping set that is a reverse copy of the original.
     *
     * @return The reversed set
     * @since 0.5.0
     */
    default MappingSet reverse() {
        return this.reverse(MappingSet.create());
    }

    @Override
    default MappingSet reverse(final MappingSet parent) {
        this.getTopLevelClassMappings().forEach(klass -> klass.reverse(parent));
        return parent;
    }

    /**
     * Produces a new mapping set, that is a merged copy with the provided
     * mappings.
     *
     * @param with The set to merge with
     * @return The merged set
     * @since 0.5.0
     */
    default MappingSet merge(final MappingSet with) {
        return this.merge(with, MappingSet.create());
    }

    /**
     * Produces a new mapping set, that is a merged copy with the provided
     * mappings.
     *
     * @param with The set to merge with
     * @param parent The set to create entries
     * @return The merged set
     * @since 0.5.0
     */
    default MappingSet merge(final MappingSet with, final MappingSet parent) {
        return MappingSetMerger.create(this, with).merge(parent);
    }

    /**
     * Produces a new mapping set, which is a clone copy of the original.
     *
     * @return The cloned set
     * @since 0.5.0
     */
    default MappingSet copy() {
        final MappingSet mappings = MappingSet.create();
        this.getTopLevelClassMappings().forEach(klass -> klass.copy(mappings));
        return mappings;
    }

}
