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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import nilloader.impl.lib.bombe.type.signature.FieldSignature;
import nilloader.impl.lib.bombe.type.signature.MethodSignature;

/**
 * An inheritance provider stores inheritance information on classes, which
 * will be obtained upon request (if not present in the cache) as opposed
 * to all in one bulk operation.
 *
 * @author Jamie Mansfield
 * @since 0.1.0
 */
public interface InheritanceProvider {

    /**
     * Gets the class information for the given class name, if available.
     *
     * @param klass The class name
     * @return The class information wrapped in an {@link Optional}
     */
    Optional<ClassInfo> provide(final String klass);

    /**
     * Gets the class information for the given class name and optional context, if available.
     *
     * <p>The provided context may be used by the {@link InheritanceProvider} to avoid
     * looking up the class by its name. The accepted values are specific to each
     * {@link InheritanceProvider}; unknown context should be ignored.</p>
     *
     * @param klass The class name
     * @param context Additional context related to the class name
     * @return The class information wrapped in an {@link Optional}
     * @since 0.3.0
     */
    default Optional<ClassInfo> provide(final String klass, final Object context) {
        return this.provide(klass);
    }

    /**
     * A wrapper used to store inheritance information about classes.
     */
    interface ClassInfo {

        /**
         * Gets the name of the class.
         *
         * @return The class' name
         */
        String getName();

        /**
         * Gets the package name of the class.
         *
         * @return The package name
         * @since 0.3.0
         */
        default String getPackage() {
            final String name = this.getName();
            final int classIndex = name.lastIndexOf('/');
            return classIndex >= 0 ? name.substring(0, classIndex) : "";
        }

        /**
         * Gets whether the represented class is an interface.
         *
         * @return {@code true} if the class is an interface
         * @since 0.3.0
         */
        boolean isInterface();

        /**
         * Gets the name of this class' super class.
         *
         * <p>Returns an empty string for interfaces or {@link Object}.</p>
         *
         * @return The super name
         */
        String getSuperName();

        /**
         * Gets an unmodifiable view of all the <i>direct</i> interfaces of the class.
         *
         * @return The class' interfaces
         */
        List<String> getInterfaces();

        /**
         * Gets an unmodifiable view of all fields declared in the class.
         * It does not include fields inherited from parent classes.
         *
         * @return The declared fields
         * @since 0.3.0
         */
        Map<FieldSignature, InheritanceType> getFields();

        /**
         * Gets an unmodifiable view of all field names declared in the class.
         * It does not include fields inherited from parent classes.
         *
         * @return The declared field names
         * @since 0.3.0
         */
        Map<String, InheritanceType> getFieldsByName();

        /**
         * Gets an unmodifiable view of all methods declared in the class.
         * It does not include methods inherited from parent classes.
         *
         * @return The declared methods
         * @since 0.3.0
         */
        Map<MethodSignature, InheritanceType> getMethods();

        /**
         * Gets an unmodifiable view of all parents of this class, recursively.
         *
         * <p>If a class in the inheritance chain cannot be provided by the
         * given {@link InheritanceProvider} it will be missing in the result,
         * along with all its parent classes.</p>
         *
         * @param provider The provider to use for looking up parent classes
         * @return A set with all parents of the class (recursively)
         * @since 0.3.0
         */
        default Set<ClassInfo> provideParents(final InheritanceProvider provider) {
            final Set<ClassInfo> result = new HashSet<>();
            this.provideParents(provider, result);
            return Collections.unmodifiableSet(result);
        }

        /**
         * Populates the given collection with all parents of this class, recursively.
         *
         * <p>If a class in the inheritance chain cannot be provided by the
         * given {@link InheritanceProvider} it will be missing in the result,
         * along with all its parent classes.</p>
         *
         * @param provider The provider to use for looking up parent classes
         * @param parents The collection to populate
         * @since 0.3.0
         */
        default void provideParents(final InheritanceProvider provider, final Collection<ClassInfo> parents) {
            provider.provide(this.getSuperName()).ifPresent(p -> {
                parents.add(p);
                p.provideParents(provider, parents);
            });
            for (final String iface : this.getInterfaces()) {
                provider.provide(iface).ifPresent(p -> {
                    parents.add(p);
                    p.provideParents(provider, parents);
                });
            }
        }

        /**
         * Returns whether this class has another class as a parent.
         *
         * <p>This method may return unexpected results if a class in
         * the inheritance chain cannot be provided by the given
         * {@link InheritanceProvider}.</p>
         *
         * @param klass The class to search in the parents of this class
         * @param provider The provider to use for looking up parent classes
         * @return {@code true} if this class inherits from the specified class
         * @since 0.3.0
         */
        default boolean hasParent(final String klass, final InheritanceProvider provider) {
            return this.provideParents(provider).stream()
                    .map(ClassInfo::getName)
                    .anyMatch(Predicate.isEqual(klass));
        }

        /**
         * Returns whether this class has another class as a parent.
         *
         * <p>This method may return unexpected results if a class in
         * the inheritance chain cannot be provided by the given
         * {@link InheritanceProvider}.</p>
         *
         * @param info The class to search in the parents of this class
         * @param provider The provider to use for looking up parent classes
         * @return {@code true} if this class inherits from the specified class
         * @since 0.3.0
         */
        default boolean hasParent(final ClassInfo info, final InheritanceProvider provider) {
            return this.provideParents(provider).contains(info);
        }

        /**
         * Returns the {@link InheritanceType} of a field declared in this class
         * that matches the given {@link FieldSignature}.
         *
         * @param field The field signature
         * @return The inheritance type or {@link InheritanceType#NONE}
         * @since 0.3.0
         */
        default InheritanceType getField(final FieldSignature field) {
            if (!field.getType().isPresent()) {
                return this.getFieldsByName().getOrDefault(field.getName(), InheritanceType.NONE);
            }
            return this.getFields().getOrDefault(field, InheritanceType.NONE);
        }

        /**
         * Returns the {@link InheritanceType} of a method declared in this class
         * that matches the given {@link MethodSignature}.
         *
         * @param method The method signature
         * @return The inheritance type or {@link InheritanceType#NONE}
         * @since 0.3.0
         */
        default InheritanceType getMethod(final MethodSignature method) {
            return this.getMethods().getOrDefault(method, InheritanceType.NONE);
        }

        /**
         * Returns whether the given child class could inherit the given field
         * from this parent class.
         *
         * <p>Note: This method does not check if the given class actually
         * extends this class or interface.
         * Use {@link #hasParent(ClassInfo, InheritanceProvider)} to check this
         * additionally if necessary.</p>
         *
         * @param child The child class to check
         * @param field The field to check
         * @return {@code true} if the child class could inherit the method
         * @since 0.3.0
         */
        default boolean canInherit(final ClassInfo child, final FieldSignature field) {
            return this.getField(field).canInherit(this, child);
        }

        /**
         * Returns whether the given child class could inherit the given method
         * from this parent class.
         *
         * <p>Note: This method does not check if the given class actually
         * extends this class or interface.
         * Use {@link #hasParent(ClassInfo, InheritanceProvider)} to check this
         * additionally if necessary.</p>
         *
         * @param child The child class to check
         * @param method The method to check
         * @return {@code true} if the child class could inherit the method
         * @since 0.3.0
         */
        default boolean canInherit(final ClassInfo child, final MethodSignature method) {
            return this.getMethod(method).canInherit(this, child);
        }

        /**
         * Returns whether this class overrides the specified method in the
         * given parent class.
         *
         * <p>Note: This method does not check if the given class actually
         * extends this class or interface.
         * Use {@link #hasParent(ClassInfo, InheritanceProvider)} to check this
         * additionally if necessary.</p>
         *
         * @param method The method to check
         * @param parent The parent class to check
         * @return {@code true} if this class overrides the method
         * @since 0.3.0
         */
        default boolean overrides(MethodSignature method, ClassInfo parent) {
            final InheritanceType own = getMethods().getOrDefault(method, InheritanceType.NONE);
            if (own == InheritanceType.NONE) return false;

            final InheritanceType parentType = parent.getMethods().getOrDefault(method, InheritanceType.NONE);
            return own.compareTo(parentType) >= 0 && parentType.canInherit(parent, this);
        }

        /**
         * Returns a new {@link ClassInfo} that caches the information
         * returned by the getters in this interface.
         *
         * <p>This method is intended for usage by {@link InheritanceProvider}s
         * to simplify their implementation. All {@link ClassInfo} provided
         * by an {@link InheritanceProvider} <i>should</i> be lazy initialized
         * or otherwise cached.</p>
         *
         * @return A lazy class info
         * @since 0.3.0
         */
        default ClassInfo lazy() {
            return new LazyInheritanceClassInfo(this);
        }

        /**
         * Abstract base implementation for {@link ClassInfo} that provides
         * a standard implementation of {@link #equals(Object)},
         * {@link #hashCode()} and {@link #toString()}.
         *
         * <p>All {@link ClassInfo} <i>should</i> implement these methods
         * as specified in this class.</p>
         *
         * @since 0.3.0
         */
        abstract class Abstract implements ClassInfo {

            @Override
            public final boolean equals(final Object o) {
                if (this == o) return true;
                if (!(o instanceof InheritanceProvider)) return false;
                final ClassInfo that = (ClassInfo) o;
                return Objects.equals(this.getName(), that.getName());
            }

            @Override
            public final int hashCode() {
                return getName().hashCode();
            }

            @Override
            public String toString() {
                return "ClassInfo{" +
                        "name='" + getName() + '\'' +
                        ", interface=" + isInterface() +
                        ", superName='" + getSuperName() + '\'' +
                        ", interfaces=" + getInterfaces() +
                        ", fields=" + getFields() +
                        ", methods=" + getMethods() +
                        '}';
            }

        }

        /**
         * A default, simple implementation of {@link ClassInfo}.
         */
        class Impl extends Abstract implements ClassInfo {

            protected final String name;
            protected final boolean isInterface;
            protected final String superName;
            protected final List<String> interfaces;
            protected final Map<FieldSignature, InheritanceType> fields;
            protected final Map<String, InheritanceType> fieldsByName;
            protected final Map<MethodSignature, InheritanceType> methods;

            protected Set<ClassInfo> parents;

            public Impl(final String name, boolean isInterface, final String superName, List<String> interfaces,
                    Map<FieldSignature, InheritanceType> fields, Map<String, InheritanceType> fieldsByName,
                    Map<MethodSignature, InheritanceType> methods) {
                this.name = name;
                this.isInterface = isInterface;
                this.superName = superName != null ? superName : "";
                this.interfaces = Collections.unmodifiableList(interfaces);
                this.fields = Collections.unmodifiableMap(fields);
                this.fieldsByName = Collections.unmodifiableMap(fieldsByName);
                this.methods = Collections.unmodifiableMap(methods);
            }

            @Override
            public String getName() {
                return this.name;
            }

            @Override
            public boolean isInterface() {
                return this.isInterface;
            }

            @Override
            public String getSuperName() {
                return this.superName;
            }

            @Override
            public List<String> getInterfaces() {
                return this.interfaces;
            }

            @Override
            public Map<FieldSignature, InheritanceType> getFields() {
                return this.fields;
            }

            @Override
            public Map<String, InheritanceType> getFieldsByName() {
                return this.fieldsByName;
            }

            @Override
            public Map<MethodSignature, InheritanceType> getMethods() {
                return this.methods;
            }

            @Override
            public Set<ClassInfo> provideParents(final InheritanceProvider provider) {
                if (this.parents == null) {
                    ClassInfo.super.provideParents(provider, this.parents = new HashSet<>());
                }
                return this.parents;
            }

            @Override
            public void provideParents(final InheritanceProvider provider, final Collection<ClassInfo> parents) {
                parents.addAll(this.provideParents(provider));
            }

            @Override
            public ClassInfo lazy() {
                return this; // Impl has all values computed already
            }

        }

    }

}
