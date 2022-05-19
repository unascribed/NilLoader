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

import java.lang.reflect.Modifier;

/**
 * Represents the (access) type of inheritance used by a specific member.
 * It is used to check if an inherited class could access a specific member
 * from a parent class.
 *
 * @author Minecrell
 * @since 0.3.0
 */
public enum InheritanceType {

    NONE,
    PACKAGE_PRIVATE,
    PROTECTED,
    PUBLIC;

    /**
     * Returns whether the given child class could access a specific member
     * from the given parent class.
     *
     * @param parent The parent class
     * @param child The child class
     * @return {@code true} if the child class could access the member
     */
    public boolean canInherit(final InheritanceProvider.ClassInfo parent, final InheritanceProvider.ClassInfo child) {
        return this != NONE && (this != PACKAGE_PRIVATE || parent.getPackage().equals(child.getPackage()));
    }

    /**
     * Returns the appropriate {@link InheritanceType} for the given modifiers
     * of a member.
     *
     * @param modifiers The modifiers of the member
     * @return The inheritance type
     * @see Modifier
     */
    public static InheritanceType fromModifiers(final int modifiers) {
        if ((modifiers & Modifier.PUBLIC) != 0) {
            return PUBLIC;
        }
        else if ((modifiers & Modifier.PROTECTED) != 0) {
            return PROTECTED;
        }
        else if ((modifiers & Modifier.PRIVATE) == 0) {
            return PACKAGE_PRIVATE;
        }
        else {
            return NONE;
        }
    }

}
