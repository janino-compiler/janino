
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2016 Arno Unkrig. All rights reserved.
 * Copyright (c) 2015-2016 TIBCO Software Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.codehaus.commons.compiler.jdk;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.codehaus.commons.nullanalysis.Nullable;

/**
 * Utility methods around the {@link java.lang.ClassLoader}.
 */
public final
class ClassLoaders {

    private
    ClassLoaders() {}

    /**
     * Equivalent with {@link #getSubresources(ClassLoader, String, boolean, boolean)} with the <var>recurse</var>
     * parameter set to {@code true}.
     */
    public static Map<String, URL>
    getSubresources(@Nullable ClassLoader classLoader, String name, boolean includeDirectories) throws IOException {
        return ClassLoaders.getSubresources(classLoader, name, includeDirectories, true);
    }

    /**
     * Returns a name-to-URL mapping of all resources "under" a given directory name.
     * <p>
     *   Iff the <var>name</var> does not end with a slash, then calling this method is equivalent with calling
     *   {@link ClassLoader#getResource(String)}.
     * </p>
     * <p>
     *   Otherwise, if the <var>name</var> <em>does</em> end with a slash, then this method returns a name-to-URL
     *   mapping of all content resources who's names <em>begin</em> with the given <var>name</var>.
     *   Iff <var>recurse</var> is {@code false}, then only <em>immediate</em> subresources are included.
     *   Iff <var>includeDirectories</var> is {@code true}, then also directory resources are included in the result
     *   set; their names all ending with a slash.
     * </p>
     * <p>
     *   If multiple resources have the <var>name</var>, then the resources are retrieved from the <var>first</var>
     *   occurrence.
     * </p>
     *
     * @param classLoader The class loader to use; {@code null} means use the system class loader
     * @param name        No leading slash
     * @return            Keys ending with a slash map to "directory resources", the other keys map to "content
     *                    resources"
     */
    public static Map<String, URL>
    getSubresources(@Nullable ClassLoader classLoader, String name, boolean includeDirectories, boolean recurse)
    throws IOException {

        if (classLoader == null) classLoader = ClassLoader.getSystemClassLoader();
        assert classLoader != null;

        URL r = classLoader.getResource(name);
        if (r == null) {
            if (name.startsWith("java/") && name.endsWith("/")) {

                // The "rt.jar" (the basic source of the BOOTCLASS) lacks directory entries; to work around this we
                // have to get well-known resource. After that, we can list the JAR.
                r = classLoader.getResource("java/lang/Object.class");
                if (r != null) {
                    String protocol = r.getProtocol();
                    if ("jar".equalsIgnoreCase(protocol)) {

                        JarURLConnection juc = (JarURLConnection) r.openConnection();
                        juc.setUseCaches(false);

                        URL      jarFileUrl = juc.getJarFileURL();
                        JarFile  jarFile    = juc.getJarFile();

                        Map<String, URL> result = ClassLoaders.getSubresources(
                            jarFileUrl,
                            jarFile,
                            name,
                            includeDirectories,
                            recurse
                        );

                        return result;
                    }
                }
            }
            return Collections.emptyMap();
        }

        return ClassLoaders.getSubresourcesOf(r, name, includeDirectories, recurse);
    }

    /**
     * Equivalent with {@link #getSubresourcesOf(URL, String, boolean, boolean)} with the <var>recurse</var>
     * parameter set to {@code true}.
     */
    public static Map<String, URL>
    getSubresourcesOf(URL root, String rootName, boolean includeDirectories) throws IOException {
        return ClassLoaders.getSubresourcesOf(root, rootName, includeDirectories, true);
    }

    /**
     * Returns a name-to-URL mapping of all resources "under" a given root resource.
     * <p>
     *   If the <var>root</var> designates a "content resource" (as opposed to a "directory resource"), then the
     *   method returns {@code Collections.singletonMap(name, rootName)}.
     * </p>
     * <p>
     *   Otherwise, if the <var>root</var> designates a "directory resource", then this method returns a name-to-URL
     *   mapping of all content resources that are located "under" the root resource.
     *   Iff <var>recurse</var> is {@code false}, then only <em>immediate</em> subresources are included.
     *   Iff <var>includeDirectories</var> is {@code true}, then directory resources are also included in the result
     *   set; their names all ending with a slash.
     * </p>
     *
     * @return Keys ending with a slash map to "directory resources", the other keys map to "content resources"
     */
    public static Map<String, URL>
    getSubresourcesOf(URL root, String rootName, boolean includeDirectories, boolean recurse) throws IOException {

        String protocol = root.getProtocol();
        if ("jar".equalsIgnoreCase(protocol)) {

            JarURLConnection juc = (JarURLConnection) root.openConnection();
            juc.setUseCaches(false);

            if (!juc.getJarEntry().isDirectory()) return Collections.singletonMap(rootName, root);

            URL     jarFileUrl = juc.getJarFileURL();
            JarFile jarFile    = juc.getJarFile();

            Map<String, URL> result = ClassLoaders.getSubresources(
                jarFileUrl,
                jarFile,
                rootName,
                includeDirectories,
                recurse
            );

            if (includeDirectories) result.put(rootName, root);

            return result;
        }

        if ("file".equalsIgnoreCase(protocol)) {
            return ClassLoaders.getFileResources(root, rootName, includeDirectories, recurse);
        }

        return Collections.singletonMap(rootName, root);
    }

    private static Map<String, URL>
    getSubresources(URL jarFileUrl, JarFile jarFile, String namePrefix, boolean includeDirectories, boolean recurse) {

        Map<String, URL> result = new HashMap<String, URL>();
        for (JarEntry je : Collections.list(jarFile.entries())) {
            if (
                (!je.isDirectory() || includeDirectories)
                && je.getName().startsWith(namePrefix)
                && (recurse || je.getName().indexOf('/', namePrefix.length()) == -1)
            ) {

                URL url;
                try {
                    url = new URL("jar", null, jarFileUrl.toString() + "!/" + je.getName());
                } catch (MalformedURLException mue) {
                    throw new AssertionError(mue);
                }

                result.put(je.getName(), url);
            }
        }
        return result;
    }

    private static Map<String, URL>
    getFileResources(URL fileUrl, String namePrefix, boolean includeDirectories, boolean recurse) {

        File file = new File(fileUrl.getFile());

        if (file.isFile()) return Collections.singletonMap(namePrefix, fileUrl);

        if (file.isDirectory()) {
            if (!namePrefix.isEmpty() && !namePrefix.endsWith("/")) namePrefix += '/';

            Map<String, URL> result = new HashMap<String, URL>();

            if (includeDirectories) result.put(namePrefix, fileUrl);

            for (File member : file.listFiles()) {
                String memberName = namePrefix + member.getName();
                URL    memberUrl  = ClassLoaders.fileUrl(member);

                if (recurse) {
                    result.putAll(ClassLoaders.getFileResources(memberUrl, memberName, includeDirectories, recurse));
                } else {
                    if (member.isFile()) result.put(memberName, memberUrl);
                }
            }

            return result;
        }

        return Collections.emptyMap();
    }

    private static URL
    fileUrl(File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException mue) {
            throw new AssertionError(mue);
        }
    }
}
