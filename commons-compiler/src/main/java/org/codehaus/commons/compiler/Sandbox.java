
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2017 Arno Unkrig. All rights reserved.
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

package org.codehaus.commons.compiler;

import java.security.AccessControlContext;
import java.security.Permission;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.codehaus.commons.nullanalysis.Nullable;

/**
 * This class establishes a security manager that confines the permissions for code executed through classes that are
 * loaded through specific class loaders.
 * <p>
 *   "To execute through a class" means that the execution stack includes the class. E.g., if a method of class {@code
 *   A} invokes a method of class {@code B}, which then invokes a method of class {@code C}, and any of the three
 *   classes were loaded by a previously {@link #confine(ClassLoader, Permissions) confined} class loader, then for
 *   all actions that are executed by class {@code C} the <i>intersection</i> of the three {@link Permissions}
 *   applies.
 * </p>
 * <p>
 *   Once the permissions for a class loader are confined, they cannot be changed; this prevents any attempts (e.g.
 *   of a confined class itself) to change the confinement.
 * </p>
 * <p>
 *   This class is a stripped-down copy of {@code de.unkrig.commons.lang.security.Sandbox}.
 * </p>
 */
public final
class Sandbox {

    private Sandbox() {}

    private static final Map<ClassLoader, AccessControlContext>
    CONFINED_CLASS_LOADERS = Collections.synchronizedMap(new WeakHashMap<ClassLoader, AccessControlContext>());

    static {

        // Install our custom security manager.
        final SecurityManager previousSecurityManager = System.getSecurityManager();

        System.setSecurityManager(new SecurityManager() {

            @Override public void
            checkPermission(@Nullable Permission perm) {
                assert perm != null;

                if (previousSecurityManager != null) previousSecurityManager.checkPermission(perm);

                final Class<?> myClass = this.getClass();

                Class<?>[] classContext = this.getClassContext();

                // Skip the first frame of the execution stack, because that is THIS class.
                for (int i = 1; i < classContext.length; i++) {
                    Class<?> clasS = classContext[i];

                    // Prevent endless recursion when we call "getClassLoader()", below, which
                    // itself indirectly calls "SecurityManager.checkPermission()".
                    if (clasS == myClass) return;

                    // Check if an ACC was set for the class loader.
                    AccessControlContext
                    acc = Sandbox.CONFINED_CLASS_LOADERS.get(clasS.getClassLoader());

                    if (acc != null) acc.checkPermission(perm);
                }
            }
        });
    }

    // --------------------------

    /**
     * All future actions that are executed through classes that were loaded through the given <var>classLoader</var>
     * will be checked against the given <var>accessControlContext</var>.
     */
    public static void
    confine(ClassLoader classLoader, AccessControlContext accessControlContext) {

        if (Sandbox.CONFINED_CLASS_LOADERS.containsKey(classLoader)) {
            throw new SecurityException("Attempt to change the access control context for '" + classLoader + "'");
        }

        Sandbox.CONFINED_CLASS_LOADERS.put(classLoader, accessControlContext);
    }

    /**
     * All future actions that are executed through classes that were loaded through the given <var>classLoader</var>
     * will be checked against the given <var>protectionDomain</var>.
     *
     * @throws SecurityException Permissions are already confined for the <var>classLoader</var>
     */
    public static void
    confine(ClassLoader classLoader, ProtectionDomain protectionDomain) {
        Sandbox.confine(classLoader, new AccessControlContext(new ProtectionDomain[] { protectionDomain }));
    }

    /**
     * All future actions that are executed through classes that were loaded through the given <var>classLoader</var>
     * will be checked against the given <var>permissions</var>.
     *
     * @throws SecurityException Permissions are already confined for the <var>classLoader</var>
     */
    public static void
    confine(ClassLoader classLoader, Permissions permissions) {
        Sandbox.confine(classLoader, new ProtectionDomain(null, permissions));
    }
}
