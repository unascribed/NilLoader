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

import nilloader.impl.lib.bombe.type.FieldType;
import nilloader.impl.lib.bombe.type.MethodDescriptor;
import nilloader.impl.lib.bombe.type.signature.FieldSignature;
import nilloader.impl.lib.bombe.type.signature.MethodSignature;
import nilloader.impl.lib.lorenz.MappingSet;
import nilloader.impl.lib.lorenz.model.ClassMapping;
import nilloader.impl.lib.lorenz.model.FieldMapping;
import nilloader.impl.lib.lorenz.model.InnerClassMapping;
import nilloader.impl.lib.lorenz.model.MethodMapping;
import nilloader.impl.lib.lorenz.model.MethodParameterMapping;
import nilloader.impl.lib.lorenz.model.TopLevelClassMapping;

/**
 * <p>
 * This class is responsible for handling the individual merge conditions found in the default {@link MappingSetMerger}.
 * It may be simpler to override the default implementation of this class to handle any particular cases you wish to
 * modify rather than overriding {@link MappingSetMerger}.
 * </p>
 * <p>
 * All methods in this class are intended to create the <i>base-mapping only</i>. That is to say, for a merge of 2 class
 * mappings, the method should merge only the name of said class and return it, not process the rest of the class's
 * members. The {@link MappingSetMerger} will handle recursively calling individual mapping methods for the members.
 * </p>
 * <hr>
 * <p>
 * <b>Continuations and Duplicates:</b>
 * <br>
 * This class distinguishes between the normal merge case and a duplicate merge case. A normal merge case is when the
 * {@code left} mapping maps {@code A -> B} and the {@code right} mapping maps {@code B -> C}. This can be thought of
 * as a mapping chain, where the deobfuscated side of the {@code left} mapping lines up with the obfuscated side of the
 * {@code right} mapping. This is what is referred to as a <i>continuation</i>, since in this case the {@code right}
 * mapping "continues" the chain. All of the {@code right} mappings in the normal merge methods are continuations.
 * </p>
 * <p>
 * There also exists the possibility that the {@code right} mapping set will duplicate the {@code left}. That means that
 * the {@code left} mapping maps {@code A -> B}, and the {@code right} mapping maps {@code A -> C} (or {@code A -> B}
 * too, what matters is the obfuscated sides match). In this case the obfuscated side of the {@code left} mapping
 * matches the obfuscated side of the right mapping. This is what this class refers to as a <i>duplicate</i> mapping.
 * </p>
 * <p>
 * The {@code mergeDuplicate*} methods in this class have 2 {@code right} mapping parameters, one called {@code right}
 * and the other called {@code rightContinuation}. If the duplicate method is called that means a duplicate has already
 * been detected, which is why the {@code right} mapping is never {@code null}. In this situation there is still a
 * possibility of a continuation mapping existing in the {@code right} mapping set, however. Put another way, the
 * {@code left} mapping set may map {@code A -> B}, and the {@code right} mapping set may map <i>both</i> {@code A -> C}
 * <i>and</i> {@code B -> D} (or {@code B -> C}, the left side is what matters here). If this situation occurs, then the
 * {@code rightContinuation} parameter will not be {@code null} and the handler implementation will need to handle this
 * case. The default implementation just assumes the duplicated {@code right} mapping is the correct mapping and throws
 * away the {@code rightContinuation} mapping. Note that attempting to keep both will always fail (in the default case)
 * because both would eventually resolve to the same obfuscated names.
 * </p>
 *
 * @implSpec In order to properly implement this interface extra care must be taken for the
 *           {@link #addRightMethodMapping(MethodMapping, ClassMapping, MergeContext) addRightMethod()} method. This
 *           method has a special case that no other method has in the standard merge case, which is that the obfuscated
 *           types of the method descriptor may be mapped to the deobfuscated types of the left side (which has no
 *           matching mapping in this case). In order to properly map this case you must also make a best-effort to map
 *           the obfuscated types of the descriptor to the obfuscated mappings of the left side. All handler methods
 *           have a {@link MergeContext context} parameter to help with these kinds of situations.
 *
 * @see MappingSetMerger
 *
 * @author Kyle Wood
 * @since 0.5.4
 */
@SuppressWarnings("unused") // Unused method parameters exist for third-party implementations
public interface MappingSetMergerHandler {

    /**
     * Create the default mapping set merger handler implementation.
     *
     * @return The newly created mapping set merger handler.
     * @since 0.5.4
     */
    static MappingSetMergerHandler create() {
        return new MappingSetMergerHandler() {};
    }

    /*
     * Top Level Class Mapping Handlers
     */

    /**
     * Merge 2 existing top level class mappings together, creating the result in the {@code target} and returning it.
     * This method should only map the name of the class, not any members.
     *
     * @param left The left top level class mapping, never {@code null}.
     * @param right The right top level class mapping, never {@code null}.
     * @param target The mapping set to create the new class mapping in, never {@code null}.
     * @param context The {@link MergeContext} associated with this merge operation, never {@code null}.
     * @return The new top level class mapping in a merge result. Pass {@code null} to the {@link MergeResult} if the
     *         mapping wasn't merged and should be removed.
     */
    default MergeResult<TopLevelClassMapping> mergeTopLevelClassMappings(
        final TopLevelClassMapping left,
        final TopLevelClassMapping right,
        final MappingSet target,
        final MergeContext context
    ) {
        return new MergeResult<>(target.createTopLevelClassMapping(left.getObfuscatedName(), right.getDeobfuscatedName()), right);
    }

    /**
     * Merge 2 existing top level class mappings together who both refer to the same obfuscated name, creating the
     * result in the {@code target} and returning it. This method should only map the name of the class, not any
     * members.
     *
     * @implNote The default implementation views the "right" mapping as the more up-to-date method, so this situation
     *           is treated by delegating to {@link #addRightTopLevelClassMapping(TopLevelClassMapping, MappingSet, MergeContext) addRightTopLevelClassMapping()}.
     * @param left The left top level class mapping, never {@code null}.
     * @param right The right top level class mapping which duplicates the left mapping, never {@code null}.
     * @param rightContinuation The optional right top level class mapping which continues from the left mapping, can be
     *                          {@code null}. For more information on continuation mappings, see the "Continuations and
     *                          Duplicates" section in {@link MappingSetMergerHandler}.
     * @param target The mapping set to create the new class mapping in, never {@code null}.
     * @param context The {@link MergeContext} associated with this merge operation, never {@code null}.
     * @return The new top level class mapping in a merge result. Pass {@code null} to the {@link MergeResult} if the
     *         mapping wasn't merged and should be removed.
     */
    default MergeResult<TopLevelClassMapping> mergeDuplicateTopLevelClassMappings(
        final TopLevelClassMapping left,
        final TopLevelClassMapping right,
        final TopLevelClassMapping rightContinuation,
        final MappingSet target,
        final MergeContext context
    ) {
        return this.addRightTopLevelClassMapping(right, target, context);
    }

    /**
     * Handle the case where only the left top level class mapping exists, creating the result in the {@code target} and
     * returning it. This method should only map the name of the class, not any members.
     *
     * @param left The left class mapping, never {@code null}.
     * @param target The mapping set to create the new class mapping in, never {@code null}.
     * @param context The {@link MergeContext} associated with this merge operation, never {@code null}.
     * @return The new top level class mapping in a merge result. Pass {@code null} to the {@link MergeResult} if the
     *         mapping wasn't merged and should be removed.
     */
    default MergeResult<TopLevelClassMapping> addLeftTopLevelClassMapping(final TopLevelClassMapping left, final MappingSet target, final MergeContext context) {
        return new MergeResult<>(target.createTopLevelClassMapping(left.getObfuscatedName(), left.getDeobfuscatedName()));
    }

    /**
     * Handle the case where only the right top level class mapping exists, creating the result in the {@code target}
     * and returning it. This method should only map the name of the class, not any members.
     *
     * @param right The right top level class mapping, never {@code null}.
     * @param target The mapping top level set to create the new class mapping in, never {@code null}.
     * @param context The {@link MergeContext} associated with this merge operation, never {@code null}.
     * @return The new top level class mapping in a merge result. Pass {@code null} to the {@link MergeResult} if the
     *         mapping wasn't merged and should be removed.
     */
    default MergeResult<TopLevelClassMapping> addRightTopLevelClassMapping(final TopLevelClassMapping right, final MappingSet target, final MergeContext context) {
        return new MergeResult<>(target.createTopLevelClassMapping(right.getObfuscatedName(), right.getDeobfuscatedName()), right);
    }

    /*
     * Inner Class Mapping Handlers
     */

    /**
     * Merge 2 existing inner classes together, creating the result in the {@code target} and returning it. This
     * method should only map the name of the class, not any members.
     *
     * @param left The left inner class mapping, never {@code null}.
     * @param right The right inner class mapping, never {@code null}.
     * @param target The class mapping to create the new inner class mapping in, never {@code null}.
     * @param context The {@link MergeContext} associated with this merge operation, never {@code null}.
     * @return The new inner class mapping in a merge result. Pass {@code null} to the {@link MergeResult} if the
     *         mapping wasn't merged and should be removed.
     */
    default MergeResult<InnerClassMapping> mergeInnerClassMappings(
        final InnerClassMapping left,
        final InnerClassMapping right,
        final ClassMapping<?, ?> target,
        final MergeContext context
    ) {
        return new MergeResult<>(target.createInnerClassMapping(left.getObfuscatedName(), right.getDeobfuscatedName()), right);
    }

    /**
     * Merge 2 existing inner class mappings together who both refer to the same obfuscated name, creating the
     * result in the {@code target} and returning it. This method should only map the name of the class, not any
     * members.
     *
     * @implNote The default implementation views the "right" mapping as the more up-to-date method, so this situation
     *           is treated by delegating to {@link #addRightInnerClassMapping(InnerClassMapping, ClassMapping, MergeContext) addRightInnerClassMapping()}.
     * @param left The left inner class mapping, never {@code null}.
     * @param right The right inner class mapping, never {@code null}.
     * @param rightContinuation The optional right inner class mapping which continues from the left mapping, can be
     *                          {@code null}. For more information on continuation mappings, see the "Continuations and
     *                          Duplicates" section in {@link MappingSetMergerHandler}.
     * @param target The class mapping to create the new inner class mapping in, never {@code null}.
     * @param context The {@link MergeContext} associated with this merge operation, never {@code null}.
     * @return The new inner class mapping in a merge result. Pass {@code null} to the {@link MergeResult} if the
     *         mapping wasn't merged and should be removed.
     */
    default MergeResult<InnerClassMapping> mergeDuplicateInnerClassMappings(
        final InnerClassMapping left,
        final InnerClassMapping right,
        final InnerClassMapping rightContinuation,
        final ClassMapping<?, ?> target,
        final MergeContext context
    ) {
        return this.addRightInnerClassMapping(right, target, context);
    }

    /**
     * Handle the case where only the left inner class mapping exists, creating the result in the {@code target} and
     * returning it. This method should only map the name of the class, not any members.
     *
     * @param left The left class mapping, never {@code null}.
     * @param target The class mapping to create the new inner class mapping in, never {@code null}.
     * @param context The {@link MergeContext} associated with this merge operation, never {@code null}.
     * @return The new inner class mapping in a merge result. Pass {@code null} to the {@link MergeResult} if the
     *         mapping wasn't merged and should be removed.
     */
    default MergeResult<InnerClassMapping> addLeftInnerClassMapping(final InnerClassMapping left, final ClassMapping<?, ?> target, final MergeContext context) {
        return new MergeResult<>(target.createInnerClassMapping(left.getObfuscatedName(), left.getDeobfuscatedName()));
    }

    /**
     * Handle the case where only the right inner class mapping exists, creating the result in the {@code target} and
     * returning it. This method should only map the name of the class, not any members.
     *
     * @param right The right class mapping, never {@code null}.
     * @param target The class mapping to create the new inner class mapping in, never {@code null}.
     * @param context The {@link MergeContext} associated with this merge operation, never {@code null}.
     * @return The new inner class mapping in a merge result. Pass {@code null} to the {@link MergeResult} if the
     *         mapping wasn't merged and should be removed.
     */
    default MergeResult<InnerClassMapping> addRightInnerClassMapping(final InnerClassMapping right, final ClassMapping<?, ?> target, final MergeContext context) {
        return new MergeResult<>(target.createInnerClassMapping(right.getObfuscatedName(), right.getDeobfuscatedName()), right);
    }

    /*
     * Field Mapping Handlers
     */

    /**
     * <p>
     * Merge 2 existing fields together, creating the result in the {@code target} and returning it.
     * </p>
     * <p>
     * There are 2 possible ways a {@code right} field mapping can continue a {@code left} field mapping:
     * </p>
     * <ul>
     *     <li>The right field mapping's obfuscated signature matches the left field mapping's deobfuscated signature.
     *         This also includes the field type. This is the standard case.</li>
     *     <li>Only the right field mapping's obfuscated name matches the left method mapping's deobfuscated name,
     *         ignoring the type.</li>
     * </ul>
     * <p>
     * The first case is considered most correct and, and thus is considered the
     * {@link FieldMergeStrategy#STRICT strict} merge and is what maps to the parameter named {@code strictRight}. The
     * second case is found only by widening the search past a typical merge, so it's considered the
     * {@link FieldMergeStrategy#LOOSE} merge and is what maps to the parameter named {@code looseRight}.
     * </p>
     * <p>
     * <b>Note:</b> Both {@code strictRight} and {@code looseRight} may be present together at the same time, but at
     * least one will always be present.
     * </p>
     *
     * @implNote The {@code looseRight} parameter will only be checked for and included if the field mapping merge
     *           strategy is set to {@link FieldMergeStrategy#LOOSE}. This is set via {@link MergeConfig}. If the merge
     *           strategy is not {@link FieldMergeStrategy#LOOSE loose} then {@code looseRight} will always be
     *           {@code null}.
     *
     * @param left The left field mapping, never {@code null}.
     * @param strictRight The best-fit right field mapping which continues from the left mapping, can be {@code null}
     *                    if not present.
     * @param looseRight The slightly worse-fit right field mapping which continues from the left mapping, can be
     *                   {@code null} if not present. Can only be present if the field merge strategy is set to
     *                   {@link FieldMergeStrategy#LOOSE}.
     * @param target The class mapping to create the new field mapping in, never {@code null}.
     * @param context The {@link MergeContext} associated with this merge operation, never {@code null}.
     * @return The new field mapping. Return {@code null} if the mapping wasn't merged and should be removed.
     */
    default FieldMapping mergeFieldMappings(
        final FieldMapping left,
        final FieldMapping strictRight,
        final FieldMapping looseRight,
        final ClassMapping<?, ?> target,
        final MergeContext context
    ) {
        if (strictRight != null) {
            return target.createFieldMapping(left.getSignature(), strictRight.getDeobfuscatedName());
        } else {
            return target.createFieldMapping(left.getSignature(), looseRight.getDeobfuscatedName());
        }
    }

    /**
     * <p>
     * Merge 2 existing field mappings together who both refer to the same obfuscated name, creating the result in the
     * {@code target} and returning it.
     * <p>
     * There are 2 possible ways a {@code right} field mapping can duplicate a {@code left} field mapping:
     * </p>
     * <ul>
     *     <li>The right field mapping's obfuscated signature matches the left field mapping's obfuscated signature.
     *         This also includes the field's type. This is the standard case.</li>
     *     <li>Only the right field mapping's obfuscated name matches the left field mapping's obfuscated name, ignoring
     *         the type.</li>
     * </ul>
     * <p>
     * The first case is considered most correct and, and thus is considered the
     * {@link FieldMergeStrategy#STRICT strict} duplicate and is what maps to the parameter named
     * {@code strictRightDuplicate}. The second case is found only by widening the search past a typical duplicate, so
     * it's considered the {@link FieldMergeStrategy#LOOSE loose} duplicate and is what maps to the parameter named
     * {@code looseRightDuplicate}.
     * </p>
     * <p>
     * There are 2 possible ways a {@code right} field mapping can continue a {@code left} field mapping:
     * </p>
     * <ul>
     *     <li>The right field mapping's obfuscated signature matches the left field mapping's deobfuscated signature.
     *         This also includes the field's type. This is the standard case.</li>
     *     <li>Only the right field mapping's obfuscated name matches the left field mapping's deobfuscated name,
     *         ignoring the type.</li>
     * </ul>
     * <p>
     * The first case is considered most correct and, and thus is considered the
     * {@link FieldMergeStrategy#STRICT strict} continuation and is what maps to the parameter named
     * {@code strictRightContinuation}. The second case is found only by widening the search past a typical merge, so
     * it's considered the {@link FieldMergeStrategy#LOOSE} merge and is what maps to the parameter named
     * {@code looseRightContinuation}.
     * </p>
     * <p>
     * <b>Note:</b> At least one of the two parameters {@code strictRightDuplicate} or {@code looseRightDuplicate}
     * must be present for this method to be called. They both can be present together at the same time. The 2
     * continuation parameters, {@code strictRightContinuation} and {@code looseRightContinuation} may also be
     * present, either together or separate, but are not guaranteed.
     * </p>
     * <p>
     * For more information on continuations, see the "Continuations and Duplicates" section in
     * {@link MappingSetMergerHandler}
     * </p>
     *
     * @implNote <p>The default implementation views the "right" mapping as the more up-to-date method, so this situation
     *           is treated by delegating to {@link #addRightFieldMapping(FieldMapping, ClassMapping, MergeContext) addRightFieldMapping()}
     *           whichever of the 2 duplicate parameters is present, checking {@code strictRightDuplicate} first as it
     *           is the preferred of the two.</p>
     *           <br>
     *           <p>The {@code looseRightDuplicate} and {@code looseRightContinuation} parameters will only be
     *           checked for and included if the field mapping merge strategy is set to {@link FieldMergeStrategy#LOOSE}.
     *           This is set via {@link MergeConfig}. If the merge strategy is not {@link FieldMergeStrategy#LOOSE loose}
     *           then {@code looseRightDuplicate} and {@code looseRightContinuation} will always be
     *           {@code null}.</p>
     *
     * @param left The left field mapping, never {@code null}.
     * @param strictRightDuplicate The best-fit right field mapping which duplicates the left mapping, can be
     *                             {@code null} if not present.
     * @param looseRightDuplicate The slightly worse-fit right field mapping which duplicates teh left mapping, can be
     *                            {@code null} if not present. Can only be present if the field merge strategy is set to
     *                            {@link FieldMergeStrategy#LOOSE}.
     * @param strictRightContinuation The optional best-fit right field mapping which continues from the left mapping,
     *                                can be {@code null} if not present.
     * @param looseRightContinuation The optional sightly worse-fit right field mapping which continues from the right
     *                               mapping, can be {@code nul if not present}. Can only be present if the field merge
     *                               strategy is set to {@link FieldMergeStrategy#LOOSE}.
     * @param target The class mapping to create the new field mapping in, never {@code null}.
     * @param context The {@link MergeContext} associated with this merge operation, never {@code null}.
     * @return The new field mapping. Return {@code null} if the mapping wasn't merged and should be removed.
     */
    default FieldMapping mergeDuplicateFieldMappings(
        final FieldMapping left,
        final FieldMapping strictRightDuplicate,
        final FieldMapping looseRightDuplicate,
        final FieldMapping strictRightContinuation,
        final FieldMapping looseRightContinuation,
        final ClassMapping<?, ?> target,
        final MergeContext context
    ) {
        if (strictRightDuplicate != null) {
            return this.addRightFieldMapping(strictRightDuplicate, target, context);
        } else {
            return this.addRightFieldMapping(looseRightDuplicate, target, context);
        }
    }

    /**
     * Handle the case where only the left mapping exists, creating the result in the {@code target} and returning it.
     *
     * @param left The left field mapping, never {@code null}.
     * @param target The class mapping to create the new field mapping in, never {@code null}.
     * @param context The {@link MergeContext} associated with this merge operation, never {@code null}.
     * @return The new field mapping. Return {@code null} if the mapping wasn't merged and should be removed.
     */
    default FieldMapping addLeftFieldMapping(final FieldMapping left, final ClassMapping<?, ?> target, final MergeContext context) {
        return target.createFieldMapping(left.getObfuscatedName(), left.getDeobfuscatedName());
    }

    /**
     * Handle the case where only the right mapping exists, creating the result in the {@code target} and returning it.
     *
     * @param right The right field mapping, never {@code null}.
     * @param target The class mapping to create the new field mapping in, never {@code null}.
     * @param context The {@link MergeContext} associated with this merge operation, never {@code null}.
     * @return The new field mapping. Return {@code null} if the mapping wasn't merged and should be removed.
     */
    default FieldMapping addRightFieldMapping(final FieldMapping right, final ClassMapping<?, ?> target, final MergeContext context) {
        final FieldType obfuscatedType = context.getLeftReversed().deobfuscate(right.getType().orElse(null));
        return target.createFieldMapping(new FieldSignature(right.getObfuscatedName(), obfuscatedType), right.getDeobfuscatedName());
    }

    /*
     * Method Mapping Handlers
     */

    /**
     * <p>
     * Merge 2 or 3 existing method mappings together, creating the result in the {@code target} and returning it. This
     * method should only map the name of the method, not any parameters.
     * </p>
     * <br>
     * <p>
     * There are 2 possible ways a {@code right} method mapping can continue a {@code left} method mapping:
     * </p>
     * <ul>
     *     <li>The right method mapping's obfuscated signature matches the left method mapping's deobfuscated signature.
     *         This is the standard case.</li>
     *     <li>The right method mapping's obfuscated name matches the left method mapping's deobfuscated name, but the
     *         right method mapping's obfuscated descriptor matches the left mapping's obfuscated descriptor.</li>
     * </ul>
     * <p>
     * The first case is considered most correct and, and thus is considered the
     * {@link MethodMergeStrategy#STRICT strict} merge and is what maps to the parameter named {@code strictRight}. The
     * second case is found only by widening the search past a typical merge, so it's considered the
     * {@link MethodMergeStrategy#LOOSE loose} merge and is what maps to the parameter named {@code looseRight}.
     * </p>
     * <br>
     * <br>
     * <p>
     * <b>Note:</b> Both {@code strictRight} and {@code wiggleRight} may be present together at the same time, but at
     * least one will always be present.
     * </p>
     *
     * @implNote The {@code looseRight} parameter will only be checked for and included if the method mapping merge
     *           strategy is set to {@link MethodMergeStrategy#LOOSE}. This is set via {@link MergeConfig}. If the merge
     *           strategy is not {@link MethodMergeStrategy#LOOSE loose} then {@code looseRight} will always be
     *           {@code null}.
     *
     * @param left The left method mapping, never {@code null}.
     * @param strictRight The best-fit right method mapping which continues from the left mapping, can be {@code null}
     *                    if not present.
     * @param looseRight The slightly worse-fit right method mapping which continues from the left mapping, can be
     *                   {@code null} if not present. Can only be present if the method merge strategy is set to
     *                   {@link MethodMergeStrategy#LOOSE}.
     * @param target The class mapping to create the new method mapping in, never {@code null}.
     * @param context The {@link MergeContext} associated with this merge operation, never {@code null}.
     * @return The new method mapping in a merge result. Pass {@code null} to the {@link MergeResult} if the
     *         mapping wasn't merged and should be removed.
     * @see #mergeDuplicateMethodMappings(MethodMapping, MethodMapping, MethodMapping, MethodMapping, MethodMapping, ClassMapping, MergeContext)
     */
    default MergeResult<MethodMapping> mergeMethodMappings(
        final MethodMapping left,
        final MethodMapping strictRight,
        final MethodMapping looseRight,
        final ClassMapping<?, ?> target,
        final MergeContext context
    ) {
        if (strictRight != null) {
            return new MergeResult<>(target.createMethodMapping(left.getSignature(), strictRight.getDeobfuscatedName()), strictRight);
        } else {
            return new MergeResult<>(target.createMethodMapping(left.getSignature(), looseRight.getDeobfuscatedName()), looseRight);
        }
    }

    /**
     * <p>
     * Merge 2 or more existing method mappings together who all refer to the same obfuscated signature, creating the
     * result in the {@code target} and returning it. This method should only map the name of the method, not any
     * parameters.
     * </p>
     * <p>
     * There are 2 possible ways a {@code right} method mapping can duplicate a {@code left} method mapping:
     * </p>
     * <ul>
     *     <li>The right method mapping's obfuscated signature matches the left method mapping's obfuscated signature.
     *         This is the standard case.</li>
     *     <li>The right method mapping's obfuscated name matches the left method mapping's obfuscated name, but the
     *         right method mapping's obfuscated descriptor matches the left mapping's deobfuscated descriptor.</li>
     * </ul>
     * <p>
     * The first case is considered most correct and, and thus is considered the
     * {@link MethodMergeStrategy#STRICT strict} duplicate and is what maps to the parameter named
     * {@code strictRightDuplicate}. The second case is found only by widening the search past a typical duplicate, so
     * it's considered the {@link MethodMergeStrategy#LOOSE loose} duplicate and is what maps to the parameter named
     * {@code looseRightDuplicate}.
     * </p>
     * <p>
     * There are 2 possible ways a {@code right} method mapping can continue a {@code left} method mapping:
     * </p>
     * <ul>
     *     <li>The right method mapping's obfuscated signature matches the left method mapping's deobfuscated signature.
     *         This is the standard case.</li>
     *     <li>The right method mapping's obfuscated name matches the left method mapping's deobfuscated name, but the
     *         right method mapping's obfuscated descriptor matches the left mapping's obfuscated descriptor.</li>
     * </ul>
     * <p>
     * The first case is considered most correct and, and thus is considered the
     * {@link MethodMergeStrategy#STRICT strict} continuation and is what maps to the parameter named
     * {@code strictRightContinuation}. The second case is found only by widening the search past a typical merge, so
     * it's considered the {@link MethodMergeStrategy#LOOSE loose} merge and is what maps to the parameter named
     * {@code looseRightContinuation}.
     * </p>
     * <p>
     * <b>Note:</b> At least one of the two parameters {@code strictRightDuplicate} or {@code looseRightDuplicate}
     * must be present for this method to be called. They both can be present together at the same time. The 2
     * continuation parameters, {@code strictRightContinuation} and {@code looseRightContinuation} may also be
     * present, either together or separate, but are not guaranteed.
     * </p>
     * <p>
     * For more information on continuation mappings, see the "Continuations and Duplicates" section in
     * {@link MappingSetMergerHandler}
     * </p>
     *
     * @implNote <p>The default implementation views the "right" mapping as the more up-to-date method, so this situation
     *           is treated by delegating to {@link #addRightMethodMapping(MethodMapping, ClassMapping, MergeContext) addRightMethodMapping()}
     *           whichever of the 2 duplicate parameters is present, checking {@code strictRightDuplicate} first as it
     *           is the preferred of the two.</p>
     *           <br>
     *           <p>The {@code looseRightDuplicate} and {@code looseRightContinuation} parameters will only be
     *           checked for and included if the method mapping merge strategy is set to {@link MethodMergeStrategy#LOOSE}.
     *           This is set via {@link MergeConfig}. If the merge strategy is not {@link MethodMergeStrategy#LOOSE loose}
     *           then {@code looseRightDuplicate} and {@code looseRightContinuation} will always be
     *           {@code null}.</p>
     *
     * @param left The left method mapping, never {@code null}.
     * @param strictRightDuplicate The best-fit right method mapping which duplicates the left mapping, can be
     *                             {@code null} if not present.
     * @param looseRightDuplicate The slightly worse-fit right method mapping which duplicates the left mapping, can be
     *                            {@code null} if not present. Can only be present if the method merge strategy is set
     *                            to {@link MethodMergeStrategy#LOOSE}.
     * @param strictRightContinuation The optional best-fit right method mapping which continues from the left mapping,
     *                                can be {@code null} if not present.
     * @param looseRightContinuation The optional slightly worse-fit right method mapping which continues from the left
     *                               mapping, can be {@code null} if not present. Can only be present if the method
     *                               merge strategy is set to {@link MethodMergeStrategy#LOOSE}.
     * @param target The class mapping to create the new method mapping in, never {@code null}.
     * @param context The {@link MergeContext} associated with this merge operation, never {@code null}.
     * @return The new method mapping in a merge result. Pass {@code null} to the {@link MergeResult} if the
     *         mapping wasn't merged and should be removed.
     * @see #mergeMethodMappings(MethodMapping, MethodMapping, MethodMapping, ClassMapping, MergeContext)
     */
    default MergeResult<MethodMapping> mergeDuplicateMethodMappings(
        final MethodMapping left,
        final MethodMapping strictRightDuplicate,
        final MethodMapping looseRightDuplicate,
        final MethodMapping strictRightContinuation,
        final MethodMapping looseRightContinuation,
        final ClassMapping<?, ?> target,
        final MergeContext context
    ) {
        if (strictRightDuplicate != null) {
            return this.addRightMethodMapping(strictRightDuplicate, target, context);
        } else {
            return this.addRightMethodMapping(looseRightDuplicate, target, context);
        }
    }

    /**
     * Handle the case where only the left method mapping exists, creating the result in the {@code target} and
     * returning it. This method should only map the name of the method, not any parameters
     *
     * @param left The left method mapping, never {@code null}.
     * @param target The class mapping to create the new method mapping in, never {@code null}.
     * @param context The {@link MergeContext} associated with this merge operation, never {@code null}.
     * @return The new method mapping in a merge result. Pass {@code null} to the {@link MergeResult} if the
     *         mapping wasn't merged and should be removed.
     */
    default MergeResult<MethodMapping> addLeftMethodMapping(final MethodMapping left, final ClassMapping<?, ?> target, final MergeContext context) {
        return new MergeResult<>(target.createMethodMapping(left.getSignature(), left.getDeobfuscatedName()));
    }

    /**
     * Handle the case where only the right method mapping exists, creating the result in the {@code target} and
     * returning it. This method should only map the name of the method, not any parameters.
     *
     * @param right The right method mapping, never {@code null}.
     * @param target The class mapping to create the new method mapping in, never {@code null}.
     * @param context The {@link MergeContext} associated with this merge operation, never {@code null}.
     * @return The new method mapping in a merge result. Pass {@code null} to the {@link MergeResult} if the
     *         mapping wasn't merged and should be removed.
     */
    default MergeResult<MethodMapping> addRightMethodMapping(final MethodMapping right, final ClassMapping<?, ?> target, final MergeContext context) {
        // We need to make sure the method obfuscated descriptor is mapped to the left side's obfuscated types
        final MethodDescriptor obfuscatedDescriptor = context.getLeftReversed().deobfuscate(right.getDescriptor());
        return new MergeResult<>(
            target.createMethodMapping(new MethodSignature(right.getObfuscatedName(), obfuscatedDescriptor), right.getDeobfuscatedName()),
            right
        );
    }

    /*
     * Method Parameter Mapping Handlers
     */

    /**
     * Merge 2 existing parameter mappings together, creating the result in the {@code target} and returning it.
     *
     * @apiNote Unlike all of the other classes of mapping merge handlers in this class, method parameters don't have a
     *          distinction between merging parameter mappings and merging duplicate parameter mappings. this is because
     *          all parameters are mapped solely by their index, so any time both mapping sets have parameters mappings
     *          for the same method they are always a kind of "duplicate" - this method handles both cases, since they
     *          are identical.
     *
     * @param left The left parameter mapping, never {@code null}.
     * @param right The right parameter mapping, never {@code null}.
     * @param target The method mapping to create the new parameter mapping in, never {@code null}.
     * @param context The {@link MergeContext} associated with this merge operation, never {@code null}.
     * @return The new parameter mapping. Return {@code null} if the mapping wasn't merged and should be removed.
     */
    default MethodParameterMapping mergeParameterMappings(
        final MethodParameterMapping left,
        final MethodParameterMapping right,
        final MethodMapping target,
        final MergeContext context
    ) {
        return target.createParameterMapping(left.getIndex(), right.getDeobfuscatedName());
    }

    /**
     * Handle the case where only the left parameter mapping mapping exists, creating the result in the {@code target}
     * and returning it.
     *
     * @param left The left parameter mapping, never {@code null}.
     * @param target The method mapping to create the new parameter mapping in, never {@code null}.
     * @param context The {@link MergeContext} associated with this merge operation, never {@code null}.
     * @return The new parameter mapping. Return {@code null} if the mapping wasn't merged and should be removed.
     */
    default MethodParameterMapping addLeftParameterMapping(
        final MethodParameterMapping left,
        final MethodMapping target,
        final MergeContext context
    ) {
        return target.createParameterMapping(left.getIndex(), left.getDeobfuscatedName());
    }

    /**
     * Handle the case where only the right parameter mapping mapping exists, creating the result in the {@code target}
     * and returning it.
     *
     * @param right The right parameter mapping, never {@code null}.
     * @param target The method mapping to create the new parameter mapping in, never {@code null}.
     * @param context The {@link MergeContext} associated with this merge operation, never {@code null}.
     * @return The new parameter mapping. Return {@code null} if the mapping wasn't merged and should be removed.
     */
    default MethodParameterMapping addRightParameterMapping(
        final MethodParameterMapping right,
        final MethodMapping target,
        final MergeContext context
    ) {
        return target.createParameterMapping(right.getIndex(), right.getDeobfuscatedName());
    }
}
