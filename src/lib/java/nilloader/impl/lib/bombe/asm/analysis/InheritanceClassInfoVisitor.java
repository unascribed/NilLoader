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

package nilloader.impl.lib.bombe.asm.analysis;

import nilloader.api.lib.asm.ClassVisitor;
import nilloader.api.lib.asm.FieldVisitor;
import nilloader.api.lib.asm.MethodVisitor;
import nilloader.api.lib.asm.Opcodes;
import nilloader.impl.lib.bombe.analysis.InheritanceProvider;
import nilloader.impl.lib.bombe.analysis.InheritanceType;
import nilloader.impl.lib.bombe.type.signature.FieldSignature;
import nilloader.impl.lib.bombe.type.signature.MethodSignature;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class InheritanceClassInfoVisitor extends ClassVisitor {

    private String name;
    private boolean isInterface;
    private String superName;
    private List<String> interfaces = Collections.emptyList();

    private final Map<FieldSignature, InheritanceType> fields = new HashMap<>();
    private final Map<String, InheritanceType> fieldsByName = new HashMap<>();
    private final Map<MethodSignature, InheritanceType> methods = new HashMap<>();

    InheritanceClassInfoVisitor(final int api) {
        super(api);
    }

    InheritanceProvider.ClassInfo create() {
        return new InheritanceProvider.ClassInfo.Impl(this.name, this.isInterface, this.superName, this.interfaces,
                this.fields, this.fieldsByName, this.methods);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.name = name;
        this.isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
        this.superName = superName;
        this.interfaces = Arrays.asList(interfaces);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        InheritanceType type = InheritanceType.fromModifiers(access);
        this.fields.put(FieldSignature.of(name, descriptor), type);
        this.fieldsByName.put(name, type);
        return null;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        this.methods.put(MethodSignature.of(name, descriptor), InheritanceType.fromModifiers(access));
        return null;
    }

}
