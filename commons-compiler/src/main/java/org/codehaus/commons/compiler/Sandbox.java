
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2017 Arno Unkrig. All rights reserved.
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
import java.security.AccessController;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;

import org.codehaus.commons.nullanalysis.NotNullByDefault;

/**
 * Executes a {@link PrivilegedAction} or {@link PrivilegedExceptionAction} in a context with restricted permissions.
 * This is useful for executing "untrusted" code, e.g. user-provided expressions or scripts that were compiled with
 * <a href="https://janino.unkrig.de/">JANINO</a>.
 * <p>
 *   Code example:
 * </p>
 * <pre>
 *     Permissions noPermissions = new Permissions();
 *     Sandbox sandbox = new Sandbox(noPermissions);
 *     sandbox.confine(new PrivilegedExceptionAction&lt;Object>() {
 *         &#64;Override public Object run() throws Exception { new java.io.File("xxx").delete(); return null; }
 *     });
 * </pre>
 *
 * @see <a href="https://docs.oracle.com/javase/tutorial/essential/environment/security.html">ORACLE: Java Essentials:
 *      The Security Manager</a>
 */
public final
class Sandbox {

    static {

        if (System.getSecurityManager() == null) {

            // Before installing the security manager, configure a decent ("positive") policy. Otherwise a policy is
            // determine automatically as follows:
            // (1) If seccurity property "policy.provider" is set: Load a class with that name, and cast it to "Policy".
            // (2) Otherwise, use class "sun.security.provider.PolicyFile" as the policy. That class reads a plethora
            //     of "*.policy" files:
            //         jre/lib/security/java[ws].policy     (Java 6, 8)
            //         conf/security/javaws.policy          (Java 9)
            //         conf/security/java.policy            (Java 9, 10, 11, 12)
            //         conf/security/policy/[un]limited/**  (Java 9, 10, 11, 12)
            //         lib/security/default.policy          (Java 9, 10, 11, 12)
            //     That eventually leads to a very restricted policy which typically allows applications to read only
            //     a small set of system properties and nothing else.
            Policy.setPolicy(new Policy() {

                @Override @NotNullByDefault(false) public PermissionCollection
                getPermissions(CodeSource codesource) {

                    // Taken from https://github.com/elastic/elasticsearch/pull/14274, on request of
                    // https://github.com/janino-compiler/janino/issues/124:

                    // Code should not rely on this method, or at least use it correctly:
                    // https://bugs.openjdk.java.net/browse/JDK-8014008
                    // return them a new empty permissions object so jvisualvm etc work
                    for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
                        if (
                            "sun.rmi.server.LoaderHandler".equals(element.getClassName())
                            && "loadClass".equals(element.getMethodName())
                        ) return new Permissions();
                    }

                    // Return UNSUPPORTED_EMPTY_COLLECTION since it is safe.
                    return super.getPermissions(codesource);
                }

                @Override @NotNullByDefault(false) public boolean
                implies(ProtectionDomain domain, Permission permission) { return true; }
            });

            System.setSecurityManager(new SecurityManager());
        }
    }

    private final AccessControlContext accessControlContext;

    /**
     * @param permissions Will be applied on later calls to {@link #confine(PrivilegedAction)} and {@link
     *                    #confine(PrivilegedExceptionAction)}
     */
    public
    Sandbox(PermissionCollection permissions) {
        this.accessControlContext = new AccessControlContext(new ProtectionDomain[] {
            new ProtectionDomain(null, permissions)
        });
    }

    /**
     * Runs the given <var>action</var>, confined by the permissions configured through the {@link
     * #Sandbox(PermissionCollection) constructor}.
     *
     * @return The value returned by the <var>action</var>
     */
    public <R> R
    confine(PrivilegedAction<R> action) {
        return AccessController.doPrivileged(action, this.accessControlContext);
    }

    public <R> R
    confine(PrivilegedExceptionAction<R> action) throws Exception {
        try {
            return AccessController.doPrivileged(action, this.accessControlContext);
        } catch (PrivilegedActionException pae) {
            throw pae.getException();
        }
    }
}
