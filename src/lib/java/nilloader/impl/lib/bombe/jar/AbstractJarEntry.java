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

package nilloader.impl.lib.bombe.jar;

import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Represents an entry within a jar file.
 *
 * @author Jamie Mansfield
 * @since 0.3.0
 */
public abstract class AbstractJarEntry {

    protected final String name;
    protected final long time;
    private String packageName;
    private String simpleName;

    protected AbstractJarEntry(final String name, final long time) {
        this.name = name;
        this.time = time;
    }

    /**
     * Gets the fully-qualified name of the jar entry.
     *
     * @return The name
     */
    public final String getName() {
        return this.name;
    }

    /**
     * Gets the time the jar entry was last modified.
     *
     * @return The time
     */
    public final long getTime() {
        return this.time;
    }

    /**
     * Gets the package that contains the jar entry, an empty
     * string if in the root package.
     *
     * @return The package name
     */
    public final String getPackage() {
        if (this.packageName != null) return this.packageName;
        final int index = this.name.lastIndexOf('/');
        if (index == -1) return this.packageName = "";
        return this.packageName = this.name.substring(0, index);
    }

    /**
     * Gets the simple name (without any packages or extension).
     *
     * @return The simple name
     */
    public final String getSimpleName() {
        if (this.simpleName != null) return this.simpleName;
        final int packageLength = this.getPackage().isEmpty() ? -1 : this.getPackage().length();
        final int extensionLength = this.getExtension().isEmpty() ? -1 : this.getExtension().length();
        return this.simpleName = this.name.substring(
                packageLength + 1,
                this.name.length() - (extensionLength + 1)
        );
    }

    /**
     * Gets the extension of the jar entry.
     *
     * @return The extension
     */
    public abstract String getExtension();

    /**
     * Gets the contents of the jar entry.
     *
     * @return The contents
     */
    public abstract byte[] getContents();

    /**
     * Writes the jar entry to the given {@link JarOutputStream}.
     *
     * @param jos The jar output stream
     * @throws IOException If an I/O exception occurs
     */
    public final void write(final JarOutputStream jos) throws IOException {
        // Create entry
        final JarEntry entry = new JarEntry(this.name);
        entry.setTime(this.time);

        // Write entry
        jos.putNextEntry(entry);
        jos.write(this.getContents());
        jos.closeEntry();
    }

    /**
     * Processes the jar entry with the given transformer.
     *
     * @param vistor The transformer
     * @return The jar entry
     */
    public abstract AbstractJarEntry accept(final JarEntryTransformer vistor);

}
