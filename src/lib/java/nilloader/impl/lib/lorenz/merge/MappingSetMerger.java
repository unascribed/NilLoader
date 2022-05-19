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

package nilloader.impl.lib.lorenz.merge;

import nilloader.impl.lib.lorenz.MappingSet;
import nilloader.impl.lib.lorenz.impl.merge.MappingSetMergerImpl;
import nilloader.impl.lib.lorenz.model.ClassMapping;
import nilloader.impl.lib.lorenz.model.FieldMapping;
import nilloader.impl.lib.lorenz.model.InnerClassMapping;
import nilloader.impl.lib.lorenz.model.MethodMapping;
import nilloader.impl.lib.lorenz.model.MethodParameterMapping;
import nilloader.impl.lib.lorenz.model.TopLevelClassMapping;

/**
 * <p>
 * A service for merging mappings. The result of a mapping merge is a new mapping which contains the result you would
 * expect to get after using the two mappings being merged in order, left, then right.
 * </p>
 * <p>
 * Put another way, merging 2 mappings means the deobfuscated output of the left {@link MappingSet} and the obfuscated
 * input of the right {@link MappingSet} match, creating a single {@link MappingSet} with the obfuscated input of the
 * left side and the deobfuscated output of the right side.
 * </p>
 * <p>
 * More complex situations are likely to occur while merging mappings, so this table will go through the different
 * possible cases and how a mapping set merger handles them in the default implementation:
 * </p>
 * <table style="border-collapse: collapse; border: 1px solid; width: 100%;">
 *     <caption>Merge Situations</caption>
 *     <tr style="border-bottom: 1px solid;">
 *         <th style="padding: 8px; width: 18%;">Left</th>
 *         <th style="padding: 8px; width: 18%;">Right</th>
 *         <th style="padding: 8px; width: 19%;">Output</th>
 *         <th style="padding: 8px; width: 45%;">Note</th>
 *     </tr>
 *     <tr style="border-bottom: 1px solid;">
 *         <td style="padding: 8px; width: 18%;">{@code A -> B}</td>
 *         <td style="padding: 8px; width: 18%;">{@code B -> C}</td>
 *         <td style="padding: 8px; width: 19%;">{@code A -> C}</td>
 *         <td style="padding: 8px; width: 45%;">Typical case, easiest to handle</td>
 *     </tr>
 *     <tr style="border-bottom: 1px solid;">
 *         <td style="padding: 8px; width: 18%;">{@code A -> B}</td>
 *         <td style="padding: 8px; width: 18%;"><i>Missing</i></td>
 *         <td style="padding: 8px; width: 19%;">{@code A -> B}</td>
 *         <td style="padding: 8px; width: 45%;">Standalone mappings get copied</td>
 *     </tr>
 *     <tr style="border-bottom: 1px solid;">
 *         <td style="padding: 8px; width: 18%;"><i>Missing</i></td>
 *         <td style="padding: 8px; width: 18%;">{@code B -> C}</td>
 *         <td style="padding: 8px; width: 19%;">{@code B -> C}</td>
 *         <td style="padding: 8px; width: 45%;">Standalone mappings get copied</td>
 *     </tr>
 *     <tr style="border-bottom: 1px solid;">
 *         <td style="padding: 8px; width: 18%;">{@code A -> B}</td>
 *         <td style="padding: 8px; width: 18%;">{@code X -> Y}</td>
 *         <td style="padding: 8px; width: 19%;">{@code A -> B}<br>{@code X -> Y}</td>
 *         <td style="padding: 8px; width: 45%;">
 *             This is no different than the 2 above cases with missing mappings on each side. This is just meant to be
 *             a further example that if two unrelated mappings are present, a standard merger won't know how to handle
 *             them other than copying both.
 *         </td>
 *     </tr>
 *     <tr style="border-bottom: 1px solid;">
 *         <td style="padding: 8px; width: 18%;">{@code A -> B}</td>
 *         <td style="padding: 8px; width: 18%;">{@code A -> C}</td>
 *         <td style="padding: 8px; width: 19%;">{@code A -> C}</td>
 *         <td style="padding: 8px; width: 45%;">
 *             By default the right mapping is considered the "most up to date" mappings, so in the case where both
 *             mapping sets provide mappings for the same obfuscated name, the right mapping is used in the default
 *             implementation.
 *         </td>
 *     </tr>
 *     <tr>
 *         <td style="padding: 8px; width: 18%;">{@code A -> B}<br>(types and names)</td>
 *         <td style="padding: 8px; width: 18%;">{@code B -> B} (types)<br>{@code A -> C} (names)</td>
 *         <td style="padding: 8px; width: 19%;">{@code A -> B} (types)<br>{@code A -> C} (names)</td>
 *         <td style="padding: 8px; width: 45%;">
 *             This is an example of a special case situation where the left mapping only maps types, then the second
 *             mapping set only maps members, but from the expectation that the first mapping set was already applied.
 *             The default implementation should handle this case correctly.
 *         </td>
 *     </tr>
 * </table>
 * <p>
 * All of these cases can happen in a single mapping merge. That is to say, some cases may apply to some classes and
 * methods but not others, but all during a single merge. It's important to note that it's illegal to merge a
 * {@code null} mapping set with another set, both must exist. The <i>Missing</i> cases in the table refer to situations
 * where one mapping set contains entries the other doesn't.
 * </p>
 *
 * @apiNote This class was added after the {@link MappingSet#merge} and related methods were implemented. The merge
 *          methods are equivalent to this class, as they only call back to the default implementation of this class.
 * @implNote The default implementation of this interface uses a {@link MappingSetMergerHandler} to handle each specific
 *           merging situation. To modify specific instances of merging behavior, it may be simpler to override that
 *           class instead and only modify the specific type of merge you want to change.
 * @see MappingSetMergerHandler
 *
 * @author Kyle Wood
 * @since 0.5.4
 */
public interface MappingSetMerger {

    /**
     * Creates a mapping set merger, using the default merger implementation with the default
     * {@link MappingSetMergerHandler} implementation.
     *
     * @param left The {@link MappingSet} for the left side of the merge
     * @param right The {@link MappingSet} for the right side of the merge
     * @return The merger
     * @see MappingSetMerger#create(MappingSet, MappingSet, MergeConfig)
     */
    static MappingSetMerger create(final MappingSet left, final MappingSet right) {
        return MappingSetMerger.create(left, right, MergeConfig.builder().build());
    }

    /**
     * Creates a mapping set merger, using the default merger implementation with the provided
     * {@link MappingSetMergerHandler} implementation.
     *
     * @param left The {@link MappingSet} for the left side of the merge
     * @param right The {@link MappingSet} for the right side of the merge
     * @param config The {@link MergeConfig} configuration for this merge session
     * @return The merger
     * @see MappingSetMerger#create(MappingSet, MappingSet)
     */
    static MappingSetMerger create(final MappingSet left, final MappingSet right, final MergeConfig config) {
        return new MappingSetMergerImpl(left, right, config);
    }

    /**
     * Merge the two {@link MappingSet}s in this merger together, returning the result. This is effectively the same as
     * calling {@link MappingSetMerger#merge(MappingSet)} with a new mapping set and returning it.
     *
     * @return The merged mapping set
     * @see MappingSetMerger#merge(MappingSet)
     */
    default MappingSet merge() {
        return this.merge(MappingSet.create());
    }

    /**
     * Merge the two {@link MappingSet}s in this merger together into the provided {@code target}. For ease of use the
     * provided {@code target} mapping set is also returned.
     *
     * @implSpec The return value of this method should always be the same object provided in the {@code target}
     *           parameter. The result shouldn't be a copy.
     * @param target The mapping set to insert the merged mappings into
     * @return The {@code target} parameter.
     */
    MappingSet merge(final MappingSet target);

    /**
     * Merge the members of the two provided {@link TopLevelClassMapping}s together into the provided {@code target}.
     *
     * @implNote This method is used by the {@link #merge(MappingSet)} method in the default implementation, but can
     *           also be used to map a single class if called directly. If you are overriding the default implementation
     *           to change behavior, first make sure what you're trying to do can't be accomplished by overriding the
     *           {@link MappingSetMergerHandler} instead.
     * @param left The class mapping for the left side of the merge. May be {@code null}.
     * @param right The class mapping for the right side of the merge. May be {@code null}.
     * @param target The mapping set to insert the new merged mapping into. May not be {@code null}.
     * @return The new class mapping, or {@code null} if the mapping is to be removed.
     */
    TopLevelClassMapping mergeTopLevelClass(final TopLevelClassMapping left, final TopLevelClassMapping right, final MappingSet target);

    /**
     * Merge the members of the two provided {@link InnerClassMapping}s together into the provided {@code target}.
     *
     * @implNote This method is used by the {@link #mergeTopLevelClass(TopLevelClassMapping, TopLevelClassMapping, MappingSet) mergeTopLevelClass()}
     *           method in the default implementation, but can also be used to map a single inner class if called
     *           directly. If you are overriding the default implementation to change behavior, first make sure what
     *           you're trying to do can't be accomplished by overriding the {@link MappingSetMergerHandler} instead.
     * @param left The class mapping for the left side of the merge. May be {@code null}.
     * @param right The class mapping for the right side of the merge. May be {@code null}.
     * @param target The class mapping to insert the new merged mapping into. May not be {@code null}.
     * @return The new class mapping, or {@code null} if the mapping is to be removed.
     */
    InnerClassMapping mergeInnerClass(final InnerClassMapping left, final InnerClassMapping right, final ClassMapping<?, ?> target);

    /**
     * Merge the two provided {@link FieldMapping}s together into the provided {@code target}.
     *
     * @implNote This method is used by the {@link #mergeTopLevelClass(TopLevelClassMapping, TopLevelClassMapping, MappingSet) mergeTopLeveClass()}
     *           and {@link #mergeInnerClass(InnerClassMapping, InnerClassMapping, ClassMapping) mergeInnerClass()}
     *           methods in the default implementation, but can also be used to map a single field if called directly.
     *           If you are overriding the default implementation to change behavior, first make sure what you're trying
     *           to do can't be accomplished by overriding the {@link MappingSetMergerHandler} instead.
     * @param left The field mapping for the left side of the merge. May be {@code null}.
     * @param right The field mapping for the right side of the merge. May be {@code null}.
     * @param target The class mapping to insert the new merged mapping into. May not be {@code null}.
     * @return The new field mapping, or {@code null} if the mapping is to be removed.
     */
    FieldMapping mergeField(final FieldMapping left, final FieldMapping right, final ClassMapping<?, ?> target);

    /**
     * Merge the two provided {@link MethodMapping}s together into the provided {@code target}.
     *
     * @implNote This method is used by the {@link #mergeTopLevelClass(TopLevelClassMapping, TopLevelClassMapping, MappingSet) mergeTopLeveClass()}
     *           and {@link #mergeInnerClass(InnerClassMapping, InnerClassMapping, ClassMapping) mergeInnerClass()}
     *           methods in the default implementation, but can also be used to map a single method if called directly.
     *           If you are overriding the default implementation to change behavior, first make sure what you're trying
     *           to do can't be accomplished by overriding the {@link MappingSetMergerHandler} instead.
     * @param left The method mapping for the left side of the merge. May be {@code null}.
     * @param right The method mapping for the right side of the merge. May be {@code null}.
     * @param target The class mapping to insert the new merged mapping into. May not be {@code null}.
     * @return The new method mapping, or {@code null} if the mapping is to be removed.
     */
    MethodMapping mergeMethod(final MethodMapping left, final MethodMapping right, final ClassMapping<?, ?> target);

    /**
     * Merge the two provided {@link MethodParameterMapping}s together into the provided {@code target}.
     *
     * @implNote This method is used by the {@link #mergeMethod(MethodMapping, MethodMapping, ClassMapping) mergeMethod()}
     *           method in the default implementation, but can also be used to map a single method parameter if called
     *           directly. If you are overriding the default implementation to change behavior, first make sure what
     *           you're trying to do can't be accomplished by overriding the {@link MappingSetMergerHandler} instead.
     * @param left The method parameter mapping for the left side of the merge. May be {@code null}.
     * @param right The method parameter mapping for the right side of the merge. May be {@code null}.
     * @param target The method mapping to insert the new merged mapping into. May not be {@code null}.
     * @return The new method parameter mapping, or {@code null} if the mapping is to be removed.
     */
    MethodParameterMapping mergeMethodParameter(final MethodParameterMapping left, final MethodParameterMapping right, final MethodMapping target);
}
