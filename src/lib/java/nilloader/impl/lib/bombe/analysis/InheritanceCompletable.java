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

import java.util.Optional;

/**
 * Represents a class that can be completed by using resolving inheritance,
 * e.g. by inheriting attributes from parent classes.
 *
 * @author Minecrell
 * @since 0.3.0
 */
public interface InheritanceCompletable {

    /**
     * Provide the appropriate {@link InheritanceProvider.ClassInfo}
     * for this class.
     *
     * @param provider The provider to use for looking up the class
     * @param context Optional context for the inheritance provider
     * @return The inheritance class info, wrapped in an {@link Optional}
     */
    Optional<InheritanceProvider.ClassInfo> provideInheritance(final InheritanceProvider provider, final Object context);

    /**
     * Returns whether this class was already completed using inheritance.
     *
     * @return If this class is complete
     */
    boolean isComplete();

    /**
     * Attempts to complete this class using the provided
     * {@link InheritanceProvider}.
     *
     * @param provider The provider to use for looking up the class
     */
    default void complete(final InheritanceProvider provider) {
        this.complete(provider, (Object) null);
    }

    /**
     * Attempts to complete this class using the provided
     * {@link InheritanceProvider}.
     *
     * @param provider The provider to use for looking up the class
     * @param context Optional context for the inheritance provider
     */
    default void complete(final InheritanceProvider provider, final Object context) {
        if (this.isComplete()) {
            return;
        }

        final Optional<InheritanceProvider.ClassInfo> info = this.provideInheritance(provider, context);
        info.ifPresent(classInfo -> this.complete(provider, classInfo));
    }

    /**
     * Attempts to complete this class using the provided
     * {@link InheritanceProvider}.
     *
     * @param provider The provider to use for looking up parent classes
     * @param info The resolve inheritance class info for this class
     */
    void complete(final InheritanceProvider provider, final InheritanceProvider.ClassInfo info);

}
