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

package nilloader.impl.lib.lorenz.impl.merge;

import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import nilloader.impl.lib.bombe.type.signature.FieldSignature;
import nilloader.impl.lib.bombe.type.signature.MethodSignature;
import nilloader.impl.lib.lorenz.MappingSet;
import nilloader.impl.lib.lorenz.merge.FieldMergeStrategy;
import nilloader.impl.lib.lorenz.merge.MappingSetMerger;
import nilloader.impl.lib.lorenz.merge.MappingSetMergerHandler;
import nilloader.impl.lib.lorenz.merge.MergeConfig;
import nilloader.impl.lib.lorenz.merge.MergeContext;
import nilloader.impl.lib.lorenz.merge.MergeResult;
import nilloader.impl.lib.lorenz.merge.MethodMergeStrategy;
import nilloader.impl.lib.lorenz.model.ClassMapping;
import nilloader.impl.lib.lorenz.model.FieldMapping;
import nilloader.impl.lib.lorenz.model.InnerClassMapping;
import nilloader.impl.lib.lorenz.model.MethodMapping;
import nilloader.impl.lib.lorenz.model.MethodParameterMapping;
import nilloader.impl.lib.lorenz.model.TopLevelClassMapping;

/**
 * Default implementation of {@link MappingSetMerger}.
 *
 * @see MappingSetMerger
 * @see MappingSetMergerHandler
 * @see MappingSetMerger#create(MappingSet, MappingSet, MergeConfig)
 * @see MappingSetMerger#create(MappingSet, MappingSet)
 *
 * @author Kyle Wood
 * @since 0.5.4
 */
public class MappingSetMergerImpl implements MappingSetMerger {

    private final MappingSetMergerHandler handler;
    private final MethodMergeStrategy methodMergeStrategy;
    private final FieldMergeStrategy fieldMergeStrategy;

    private final MappingSet left;
    private final MappingSet right;

    private final MergeContext context;

    private final int parallelism;

    public MappingSetMergerImpl(final MappingSet left, final MappingSet right, final MergeConfig config) {
        this.left = left;
        this.right = right;
        this.handler = config.getHandler();
        this.methodMergeStrategy = config.getMethodMergeStrategy();
        this.fieldMergeStrategy = config.getFieldMergeStrategy();
        this.parallelism = config.getParallelism();

        this.context = new MergeContext(this.left, this.right);
    }

    @Override
    public MappingSet merge(final MappingSet target) {
        final HashSet<String> seenNames = new HashSet<>();

        final ExecutorService executor;
        if (this.parallelism == -1) {
            executor = Executors.newWorkStealingPool();
        } else {
            executor = Executors.newWorkStealingPool(this.parallelism);
        }

        try {
            final CompletableFuture<Void> leftFuture = CompletableFuture.allOf(this.left.getTopLevelClassMappings().stream()
                .peek(mapping -> {
                    seenNames.add(mapping.getObfuscatedName());
                    seenNames.add(mapping.getDeobfuscatedName());
                })
                .map(mapping -> CompletableFuture.runAsync(() -> {
                    final TopLevelClassMapping rightContinuation = this.right.getTopLevelClassMapping(mapping.getDeobfuscatedName()).orElse(null);
                    final TopLevelClassMapping rightDuplicate = this.right.getTopLevelClassMapping(mapping.getObfuscatedName()).orElse(null);
                    this.mergeTopLevelClassInternal(mapping, rightContinuation, rightDuplicate, target);
                }, executor))
                .toArray(CompletableFuture[]::new));

            final CompletableFuture<Void> rightFuture = CompletableFuture.allOf(this.right.getTopLevelClassMappings().stream()
                .filter(mapping -> !seenNames.contains(mapping.getObfuscatedName()))
                .map(mapping -> CompletableFuture.runAsync(() -> this.mergeTopLevelClassInternal(null, mapping, null, target), executor))
                .toArray(CompletableFuture[]::new));

            try {
                CompletableFuture.allOf(leftFuture, rightFuture).get();
            } catch (final InterruptedException | ExecutionException e) {
                throw new RuntimeException("Mapping operation failed", e);
            }
        } finally {
            executor.shutdown();
        }

        return target;
    }

    @Override
    public TopLevelClassMapping mergeTopLevelClass(final TopLevelClassMapping left, final TopLevelClassMapping right, final MappingSet target) {
        if (left != null && right != null && left.getObfuscatedName().equals(right.getObfuscatedName())) {
            return this.mergeTopLevelClassInternal(left, null, right, target);
        } else {
            return this.mergeTopLevelClassInternal(left, right, null, target);
        }
    }

    protected TopLevelClassMapping mergeTopLevelClassInternal(
        final TopLevelClassMapping left,
        final TopLevelClassMapping rightContinuation,
        final TopLevelClassMapping rightDuplicate,
        final MappingSet target
    ) {
        final MergeResult<TopLevelClassMapping> mergeResult;
        if (left != null && rightDuplicate != null) {
            mergeResult = this.handler.mergeDuplicateTopLevelClassMappings(left, rightDuplicate, rightContinuation, target, this.context);
        } else if (left != null && rightContinuation != null) {
            mergeResult = this.handler.mergeTopLevelClassMappings(left, rightContinuation, target, this.context);
        } else if (rightContinuation == null && left != null) {
            mergeResult = this.handler.addLeftTopLevelClassMapping(left, target, this.context);
        } else if (rightContinuation != null) {
            mergeResult = this.handler.addRightTopLevelClassMapping(rightContinuation, target, this.context);
        } else {
            throw new IllegalStateException("Cannot merge null mappings");
        }

        final TopLevelClassMapping newMapping = mergeResult.getResult();
        if (newMapping == null) {
            return null;
        }

        if (mergeResult.getMappingsToMap().isEmpty()) {
            this.mergeClass(left, null, newMapping);
        } else {
            for (final TopLevelClassMapping mapping : mergeResult.getMappingsToMap()) {
                this.mergeClass(left, mapping, newMapping);
            }
        }

        return newMapping;
    }

    @Override
    public InnerClassMapping mergeInnerClass(final InnerClassMapping left, final InnerClassMapping right, final ClassMapping<?, ?> target) {
        if (left != null && right != null && left.getObfuscatedName().equals(right.getObfuscatedName())) {
            return this.mergeInnerClassInternal(left, null, right, target);
        } else {
            return this.mergeInnerClassInternal(left, right, null, target);
        }
    }

    protected InnerClassMapping mergeInnerClassInternal(
        final InnerClassMapping left,
        final InnerClassMapping rightContinuation,
        final InnerClassMapping rightDuplicate,
        final ClassMapping<?, ?> target
    ) {
        final MergeResult<InnerClassMapping> mergeResult;
        if (left != null && rightDuplicate != null) {
            mergeResult = this.handler.mergeDuplicateInnerClassMappings(left, rightDuplicate, rightContinuation, target, this.context);
        } else if (left != null && rightContinuation != null) {
            mergeResult = this.handler.mergeInnerClassMappings(left, rightContinuation, target, this.context);
        } else if (rightContinuation == null && left != null) {
            mergeResult = this.handler.addLeftInnerClassMapping(left, target, this.context);
        } else if (rightContinuation != null) {
            mergeResult = this.handler.addRightInnerClassMapping(rightContinuation, target,this.context);
        } else {
            throw new IllegalStateException("Cannot merge null mappings");
        }

        final InnerClassMapping newMapping = mergeResult.getResult();
        if (newMapping == null) {
            return null;
        }

        if (mergeResult.getMappingsToMap().isEmpty()) {
            this.mergeClass(left, null, newMapping);
        } else {
            for (final InnerClassMapping mapping : mergeResult.getMappingsToMap()) {
                this.mergeClass(left, mapping, newMapping);
            }
        }

        return newMapping;
    }

    @Override
    public FieldMapping mergeField(final FieldMapping left, final FieldMapping right, final ClassMapping<?, ?> target) {
        if (left != null && right != null && left.getObfuscatedName().equals(right.getObfuscatedName())) {
            return this.mergeFieldInternal(left, null, null, right, null, target);
        } else {
            return this.mergeFieldInternal(left, right, null, null, null, target);
        }
    }

    protected FieldMapping mergeFieldInternal(
        final FieldMapping left,
        final FieldMapping strictRightContinuation,
        final FieldMapping looseRightContinuation,
        final FieldMapping strictRightDuplicate,
        final FieldMapping looseRightDuplicate,
        final ClassMapping<?, ?> target
    ) {
        if (left != null && (strictRightDuplicate != null || looseRightDuplicate != null)) {
            return this.handler.mergeDuplicateFieldMappings(
                left,
                strictRightDuplicate,
                looseRightDuplicate,
                strictRightContinuation,
                looseRightContinuation,
                target,
                this.context
            );
        } else if (left != null && (strictRightContinuation != null || looseRightContinuation != null)) {
            return this.handler.mergeFieldMappings(left, strictRightContinuation, looseRightContinuation, target, this.context);
        } else if ((strictRightContinuation == null && looseRightContinuation == null) && left != null) {
            return this.handler.addLeftFieldMapping(left, target, this.context);
        } else if (strictRightContinuation != null) {
            // If left is not null then the only possible mapping left is strict, since there's nothing to wiggle against
            return this.handler.addRightFieldMapping(strictRightContinuation, target,this.context);
        } else {
            throw new IllegalStateException("Cannot merge null mappings");
        }
    }

    @Override
    public MethodMapping mergeMethod(final MethodMapping left, final MethodMapping right, final ClassMapping<?, ?> target) {
        if (left != null && right != null && left.getSignature().equals(right.getDeobfuscatedSignature())) {
            return this.mergeMethodInternal(left, null, null, right, null, target);
        } else {
            return this.mergeMethodInternal(left, right, null, null, null, target);
        }
    }

    protected MethodMapping mergeMethodInternal(
        final MethodMapping left,
        final MethodMapping strictRightContinuation,
        final MethodMapping looseRightContinuation,
        final MethodMapping strictRightDuplicate,
        final MethodMapping looseRightDuplicate,
        final ClassMapping<?, ?> target
    ) {
        final MergeResult<MethodMapping> mergeResult;
        if (left != null && (strictRightDuplicate != null || looseRightDuplicate != null)) {
            mergeResult = this.handler.mergeDuplicateMethodMappings(
                left,
                strictRightDuplicate,
                looseRightDuplicate,
                strictRightContinuation,
                looseRightContinuation,
                target,
                this.context
            );
        } else if (left != null && (strictRightContinuation != null || looseRightContinuation != null)) {
            mergeResult = this.handler.mergeMethodMappings(left, strictRightContinuation, looseRightContinuation, target, this.context);
        } else if ((strictRightContinuation == null && looseRightContinuation == null) && left != null) {
            mergeResult = this.handler.addLeftMethodMapping(left, target, this.context);
        } else if (strictRightContinuation != null) {
            // If left is not null then the only possible mapping left is strict, since there's nothing to wiggle against
            mergeResult = this.handler.addRightMethodMapping(strictRightContinuation, target, this.context);
        } else {
            throw new IllegalStateException("Cannot merge null mappings");
        }

        final MethodMapping newMapping = mergeResult.getResult();
        if (newMapping == null) {
            return null;
        }

        if (mergeResult.getMappingsToMap().isEmpty()) {
            this.mergeMethodInto(left, null, newMapping);
        } else {
            for (final MethodMapping mapping : mergeResult.getMappingsToMap()) {
                this.mergeMethodInto(left, mapping, newMapping);
            }
        }

        return newMapping;
    }

    protected void mergeMethodInto(final MethodMapping left, final MethodMapping right, final MethodMapping newMapping) {
        final HashSet<Integer> seenIndexes = new HashSet<>();

        if (left != null) {
            for (final MethodParameterMapping leftMapping : left.getParameterMappings()) {
                final MethodParameterMapping rightMapping;
                if (right != null) {
                    rightMapping = right.getParameterMapping(leftMapping.getIndex()).orElse(null);
                } else {
                    rightMapping = null;
                }
                this.mergeMethodParameter(leftMapping, rightMapping, newMapping);
                seenIndexes.add(leftMapping.getIndex());
            }
        }
        if (right != null) {
            for (final MethodParameterMapping rightMapping : right.getParameterMappings()) {
                if (!seenIndexes.contains(rightMapping.getIndex())) {
                    this.mergeMethodParameter(null, rightMapping, newMapping);
                }
            }
        }
    }

    @Override
    public MethodParameterMapping mergeMethodParameter(
        final MethodParameterMapping left,
        final MethodParameterMapping right,
        final MethodMapping target
    ) {
        if (left != null && right != null) {
            return this.handler.mergeParameterMappings(left, right, target, this.context);
        } else if (right == null && left != null) {
            return this.handler.addLeftParameterMapping(left, target, this.context);
        } else if (right != null) {
            return this.handler.addRightParameterMapping(right, target, this.context);
        } else {
            throw new IllegalStateException("Cannot merge 2 null mappings");
        }
    }

    protected <T extends ClassMapping<T, ?>> void mergeClass(final T left, final T right, final T newMapping) {
        final HashSet<String> seenClasses = new HashSet<>();
        final HashSet<FieldSignature> seenFields = new HashSet<>();
        final HashSet<String> seenFieldNames = new HashSet<>();
        final HashSet<MethodSignature> seenMethods = new HashSet<>();

        // Classes
        if (left != null) {
            for (final InnerClassMapping leftMapping : left.getInnerClassMappings()) {
                final InnerClassMapping rightContinuation;
                final InnerClassMapping rightDuplicate;
                if (right != null) {
                    rightContinuation = right.getInnerClassMapping(leftMapping.getDeobfuscatedName()).orElse(null);
                    rightDuplicate = right.getInnerClassMapping(leftMapping.getObfuscatedName()).orElse(null);
                } else {
                    rightContinuation = null;
                    rightDuplicate = null;
                }
                this.mergeInnerClassInternal(leftMapping, rightContinuation, rightDuplicate, newMapping);
                seenClasses.add(leftMapping.getObfuscatedName());
                seenClasses.add(leftMapping.getDeobfuscatedName());
            }
        }
        if (right != null) {
            for (final InnerClassMapping rightMapping : right.getInnerClassMappings()) {
                if (!seenClasses.contains(rightMapping.getObfuscatedName())) {
                    this.mergeInnerClassInternal(null, rightMapping, null, newMapping);
                }
            }
        }

        // Fields
        if (left != null) {
            for (final FieldMapping leftMapping : left.getFieldMappings()) {
                final FieldMapping strictRightContinuation;
                final FieldMapping strictRightDuplicate;
                final FieldMapping looseRightContinuation;
                final FieldMapping looseRightDuplicate;

                if (right != null) {
                    strictRightContinuation = right.getFieldMapping(leftMapping.getDeobfuscatedSignature()).orElse(null);
                    strictRightDuplicate = right.getFieldMapping(leftMapping.getSignature()).orElse(null);

                    if (this.fieldMergeStrategy == FieldMergeStrategy.LOOSE) {
                        // We filter out loose matches which simply match to the same instance as those aren't actually loose
                        looseRightContinuation = right.getFieldMapping(leftMapping.getDeobfuscatedName())
                            .filter(m -> !m.equals(strictRightContinuation)).orElse(null);
                        looseRightDuplicate = right.getFieldMapping(leftMapping.getObfuscatedName())
                            .filter(m -> !m.equals(strictRightDuplicate)).orElse(null);
                    } else {
                        looseRightContinuation = null;
                        looseRightDuplicate = null;
                    }
                } else {
                    strictRightContinuation = null;
                    strictRightDuplicate = null;
                    looseRightContinuation = null;
                    looseRightDuplicate = null;
                }

                this.mergeFieldInternal(
                    leftMapping,
                    strictRightContinuation,
                    looseRightContinuation,
                    strictRightDuplicate,
                    looseRightDuplicate,
                    newMapping
                );
                seenFields.add(leftMapping.getSignature());
                seenFields.add(leftMapping.getDeobfuscatedSignature());

                if (this.fieldMergeStrategy == FieldMergeStrategy.LOOSE) {
                    seenFieldNames.add(leftMapping.getObfuscatedName());
                    seenFieldNames.add(leftMapping.getDeobfuscatedName());
                }
            }
        }
        if (right != null) {
            for (final FieldMapping rightMapping : right.getFieldMappings()) {
                if (!seenFieldNames.contains(rightMapping.getObfuscatedName()) && !seenFields.contains(rightMapping.getSignature())) {
                    this.mergeFieldInternal(null, rightMapping, null, null, null, newMapping);
                }
            }
        }

        // Methods
        if (left != null) {
            for (final MethodMapping leftMapping : left.getMethodMappings()) {
                // There are 2 possible ways a method mapping can continue, and 2 possible ways a method mapping can duplicate
                final MethodMapping strictRightContinuation;
                final MethodMapping strictRightDuplicate;
                final MethodMapping looseRightContinuation;
                final MethodMapping looseRightDuplicate;

                // For loose we mix and match the parts of the signature:
                //  deobfuscated name with obfuscated descriptor <- considered continuation because name continues
                //  obfuscated name with deobfuscated descriptor <- considered duplicate because name duplicates
                final MethodSignature looseContinuationSig = new MethodSignature(leftMapping.getDeobfuscatedName(), leftMapping.getDescriptor());
                final MethodSignature looseDupSig =
                    new MethodSignature(leftMapping.getObfuscatedName(), leftMapping.getDeobfuscatedSignature().getDescriptor());

                if (right != null) {
                    strictRightContinuation = right.getMethodMapping(leftMapping.getDeobfuscatedSignature()).orElse(null);
                    strictRightDuplicate = right.getMethodMapping(leftMapping.getSignature()).orElse(null);

                    if (this.methodMergeStrategy == MethodMergeStrategy.LOOSE) {
                        looseRightContinuation = right.getMethodMapping(looseContinuationSig).orElse(null);
                        looseRightDuplicate = right.getMethodMapping(looseDupSig).orElse(null);
                    } else {
                        looseRightContinuation = null;
                        looseRightDuplicate = null;
                    }
                } else {
                    strictRightContinuation = null;
                    looseRightContinuation = null;
                    strictRightDuplicate = null;
                    looseRightDuplicate = null;
                }

                this.mergeMethodInternal(
                    leftMapping,
                    strictRightContinuation,
                    looseRightContinuation,
                    strictRightDuplicate,
                    looseRightDuplicate,
                    newMapping
                );
                seenMethods.add(leftMapping.getSignature());
                seenMethods.add(leftMapping.getDeobfuscatedSignature());

                if (this.methodMergeStrategy == MethodMergeStrategy.LOOSE) {
                    seenMethods.add(looseContinuationSig);
                    seenMethods.add(looseDupSig);
                }
            }
        }
        if (right != null) {
            for (final MethodMapping rightMapping : right.getMethodMappings()) {
                if (!seenMethods.contains(rightMapping.getSignature())) {
                    this.mergeMethodInternal(null, rightMapping, null, null, null, newMapping);
                }
            }
        }
    }
}
