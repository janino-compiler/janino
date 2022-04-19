
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2019 Arno Unkrig. All rights reserved.
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

package org.codehaus.commons.compiler.lang;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.codehaus.commons.compiler.java8.java.util.function.Consumer;
import org.codehaus.commons.compiler.java9.java.lang.module.ModuleFinder;
import org.codehaus.commons.compiler.java9.java.lang.module.ModuleReference;
import org.codehaus.commons.compiler.util.resource.LocatableResource;
import org.codehaus.commons.compiler.util.resource.Resource;
import org.codehaus.commons.compiler.util.resource.ResourceFinder;
import org.codehaus.commons.nullanalysis.NotNullByDefault;
import org.codehaus.commons.nullanalysis.Nullable;

/**
 * Utility methods around the {@link java.lang.ClassLoader}.
 */
public final
class ClassLoaders {

    /**
     * The {@link ClassLoader} that loads the classes on the currently executing JVM's "class path", i.e. the JARs in
     * the JRE's "lib" and "lib/ext" directories, and the JARs and class directories specified through the class path.
     */
    public static final ClassLoader CLASSPATH_CLASS_LOADER = ClassLoader.getSystemClassLoader();

    /**
     * The {@link ClassLoader} that loads the classes on the currently executing JVM's "boot class path", i.e. the JARs
     * in the JRE's "lib" and "lib/ext" directories, but not the JARs and class directories specified through the
     * {@code --classpath} command line option.
     */
    public static final ClassLoader BOOTCLASSPATH_CLASS_LOADER = ClassLoader.getSystemClassLoader().getParent();

    private ClassLoaders() {}

    /**
     * Creates and returns a {@link ClassLoader} that implements {@link ClassLoader#getResourceAsStream(String)} via a
     * {@link ResourceFinder}.
     * <p>
     *   {@link ClassLoader#getResource(String)} returns a non-{@code null} value iff then resoure finder finds a
     *   {@link LocatableResource}.
     * </p>
     * <p>
     *   Notice that {@link ClassLoader#getResources(String)} is <em>not</em> overridden.
     * </p>
     */
    public static ClassLoader
    getsResourceAsStream(final ResourceFinder finder, @Nullable ClassLoader parent) {

        return new ClassLoader(parent) {

            @Override @NotNullByDefault(false) public URL
            getResource(String resourceName) {

                {
                    URL result = super.getResource(resourceName);
                    if (result != null) return result;
                }

                Resource r = finder.findResource(resourceName);
                if (r == null) return null;

                if (r instanceof LocatableResource) {
                    try {
                        return ((LocatableResource) r).getLocation();
                    } catch (IOException ioe) {
                        return null;
                    }
                }

                return null;
            }

            @Override @NotNullByDefault(false) public InputStream
            getResourceAsStream(String resourceName) {

                {
                    InputStream result = super.getResourceAsStream(resourceName);
                    if (result != null) return result;
                }

                try {
                    return finder.findResourceAsStream(resourceName);
                } catch (IOException ioe) {
                    return null;
                }
            }
        };
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
    getSubresources(
        @Nullable ClassLoader classLoader,
        final String          name,
        boolean               includeDirectories,
        final boolean         recurse
    ) throws IOException {

        if (classLoader == null) classLoader = ClassLoader.getSystemClassLoader();
        assert classLoader != null;

        HashMap<String, URL> result = new HashMap<>();

        // See if there is a "directory resource" with the given name. This will work for
        //  * file-based classpath entries (e.g. "file:./target/classes/"),
        //  * jar-based classpath entries (e.g. "jar:./path/to/my.jar"), iff the .jar file has directory entries
        for (URL r : Collections.list(classLoader.getResources(name))) {
            result.putAll(ClassLoaders.getSubresourcesOf(r, name, includeDirectories, recurse));
        }

        // Optimization: Iff resources were found on the CLASSPATH, then don't check the BOOTCLASSPATH.
        if (!result.isEmpty()) return result;

        // The .jar files on the BOOTCLASSPATH lack directory entries.
        result.putAll(ClassLoaders.getBootclasspathSubresourcesOf(name, includeDirectories, recurse));

        return result;
    }

    /**
     * Finds subresources on the JVM's <em>bootstrap</em> classpath. This is kind of tricky because the .jar files on
     * the BOOTCLASSPATH don't contain "directory entries".
     */
    private static Map<? extends String, ? extends URL>
    getBootclasspathSubresourcesOf(final String name, boolean includeDirectories, final boolean recurse)
    throws IOException {
        return ClassLoaders.BOOTCLASSPATH_SUBRESOURCES_OF.get(name, includeDirectories, recurse);
    }

    /**
     * @see SubresourceGetter#get(String, boolean, boolean)
     */
    interface SubresourceGetter {

        /**
         * @return All resources that exist "under" a given resource name
         */
        Map<? extends String, ? extends URL>
        get(String name, boolean includeDirectories, boolean recurse) throws IOException;
    }

    private static final SubresourceGetter BOOTCLASSPATH_SUBRESOURCES_OF;
    static {

        // To get to the BOOCLASSPATH resources, we have to get a well-known resource. After that, we can scan the
        // BOOTCLASSPATH.
        URL r = ClassLoader.getSystemClassLoader().getResource("java/lang/Object.class");
        assert r != null;

        String protocol = r.getProtocol();
        if ("jar".equalsIgnoreCase(protocol)) {

            // Pre-Java-9 bootstrap classpath.
            final URL     jarFileURL;
            final JarFile jarFile;
            try {
                JarURLConnection juc = (JarURLConnection) r.openConnection();
                juc.setUseCaches(false);
                jarFileURL = juc.getJarFileURL();
                jarFile    = juc.getJarFile();
            } catch (IOException ioe) {
                throw new AssertionError(ioe);
            }

            BOOTCLASSPATH_SUBRESOURCES_OF = new SubresourceGetter() {

                @Override public Map<? extends String, ? extends URL>
                get(String name, boolean includeDirectories, boolean recurse) {
                    return ClassLoaders.getSubresources(jarFileURL, jarFile, name, includeDirectories, recurse);
                }
            };
        } else
        if ("jrt".equalsIgnoreCase(protocol)) {

            // Java-9+ bootstrap classpath.
            final Set<ModuleReference> mrs = ModuleFinder.ofSystem().findAll();

            BOOTCLASSPATH_SUBRESOURCES_OF = new SubresourceGetter() {

                @Override public Map<? extends String, ? extends URL>
                get(final String name, boolean includeDirectories, final boolean recurse) throws IOException {

                    final Map<String, URL> result = new HashMap<>();

                    for (final ModuleReference mr : mrs) {
                        final URI moduleContentLocation = (URI) mr.location().get();
                        mr.open().list().forEach(new Consumer<Object>() {

                            @Override public void
                            accept(Object resourceNameObject) {
                                String resourceName = (String) resourceNameObject;
                                try {
                                    this.accept2(resourceName);
                                } catch (MalformedURLException mue) {
                                    throw new AssertionError(mue);
                                }
                            }

                            public void
                            accept2(String resourceName) throws MalformedURLException {

                                // This ".class" (?) file exists in every module, and we don't want to list it.
                                if ("module-info.class".equals(resourceName)) return;
                                // Also this one:
                                if ("_imported.marker".equals(resourceName)) return;

                                if (
                                    resourceName.startsWith(name)
                                    && (recurse || resourceName.lastIndexOf('/') == name.length() - 1)
                                ) {
                                    final URL classFileUrl = new URL(moduleContentLocation + "/" + resourceName);

                                    URL prev = (URL) result.put(resourceName, classFileUrl);
                                    assert prev == null : (
                                        "prev="
                                        + prev
                                        + ", resourceName="
                                        + resourceName
                                        + ", classFileUrl="
                                        + classFileUrl
                                    );
                                }
                            }
                        });
                    }

                    return result;
                }
            };
        } else
        {
            throw new AssertionError(
                "\"java/lang/Object.class\" is not in a \"jar:\" location nor in a \"jrt:\" location"
            );
        }
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

        Map<String, URL> result = new HashMap<>();

        for (Enumeration<JarEntry> en = jarFile.entries(); en.hasMoreElements();) {
            JarEntry je = (JarEntry) en.nextElement();

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

            Map<String, URL> result = new HashMap<>();

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
