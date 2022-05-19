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
import java.util.List;
import java.util.Map;
import java.util.Set;

import nilloader.impl.lib.bombe.type.signature.FieldSignature;
import nilloader.impl.lib.bombe.type.signature.MethodSignature;

final class LazyInheritanceClassInfo extends InheritanceProvider.ClassInfo.Abstract {

    private final InheritanceProvider.ClassInfo provider;

    // Cached data
    private final String name;
    private String superName;
    private List<String> interfaces;
    private Map<FieldSignature, InheritanceType> fields;
    private Map<String, InheritanceType> fieldsByName;
    private Map<MethodSignature, InheritanceType> methods;
    private Set<InheritanceProvider.ClassInfo> parents;

    LazyInheritanceClassInfo(final InheritanceProvider.ClassInfo provider) {
        this.provider = provider;
        this.name = provider.getName();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public boolean isInterface() {
        return this.provider.isInterface();
    }

    @Override
    public String getSuperName() {
        if (this.superName == null) {
            this.superName = this.provider.getSuperName();
        }
        return this.superName;
    }

    @Override
    public List<String> getInterfaces() {
        if (this.interfaces == null) {
            this.interfaces = this.provider.getInterfaces();
        }
        return this.interfaces;
    }

    @Override
    public Map<FieldSignature, InheritanceType> getFields() {
        if (this.fields == null) {
            this.fields = this.provider.getFields();
        }
        return this.fields;
    }

    @Override
    public Map<String, InheritanceType> getFieldsByName() {
        if (this.fieldsByName == null) {
            this.fieldsByName = this.provider.getFieldsByName();
        }
        return this.fieldsByName;
    }

    @Override
    public Map<MethodSignature, InheritanceType> getMethods() {
        if (this.methods == null) {
            this.methods = this.provider.getMethods();
        }
        return this.methods;
    }

    @Override
    public Set<InheritanceProvider.ClassInfo> provideParents(final InheritanceProvider provider) {
        if (this.parents == null) {
            this.parents = this.provider.provideParents(provider);
        }
        return this.parents;
    }

    @Override
    public void provideParents(final InheritanceProvider provider, final Collection<InheritanceProvider.ClassInfo> parents) {
        parents.addAll(this.provideParents(provider));
    }

    @Override
    public InheritanceProvider.ClassInfo lazy() {
        return this;
    }

}
