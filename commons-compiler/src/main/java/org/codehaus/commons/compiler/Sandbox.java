
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
import java.util.logging.Logger;

import org.codehaus.commons.nullanalysis.NotNullByDefault;

/**
 * Executes a {@link PrivilegedAction} or {@link PrivilegedExceptionAction} in a context with restricted permissions.
 *
 * <p><b>SECURITY WARNING:</b> This class relies on {@link SecurityManager} and {@link AccessController},
 * both deprecated for removal since Java 17 (JEP 411) and removed in Java 24.
 * <b>On Java 17+, this class provides NO security protection whatsoever.</b>
 * Compiled code executed through Janino can call any API (Runtime.exec(), network, filesystem, reflection)
 * without restriction, regardless of whether this Sandbox is used.</p>
 *
 * <p>For sandboxing untrusted code on Java 17+, use process-level isolation (separate JVM),
 * container isolation, or GraalVM polyglot sandboxing instead.</p>
 *
 * @see <a href="https://openjdk.org/jeps/411">JEP 411: Deprecate the Security Manager for Removal</a>
 * @deprecated This class is non-functional on Java 17+ due to SecurityManager removal.
 *             It provides a false sense of security. Use process-level or container-level
 *             isolation for untrusted code instead.
 */
@Deprecated(since = "3.1.13", forRemoval = true)
@SuppressWarnings("removal")
public final
class Sandbox {

    private static final Logger LOGGER = Logger.getLogger(Sandbox.class.getName());

    private static final boolean SECURITY_MANAGER_AVAILABLE;

    static {
        boolean smAvailable = false;
        try {
            if (System.getSecurityManager() == null) {
                Policy.setPolicy(new Policy() {

                    @Override @NotNullByDefault(false) public PermissionCollection
                    getPermissions(CodeSource codesource) {
                        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
                            if (
                                "sun.rmi.server.LoaderHandler".equals(element.getClassName())
                                && "loadClass".equals(element.getMethodName())
                            ) return new Permissions();
                        }
                        return super.getPermissions(codesource);
                    }

                    @Override @NotNullByDefault(false) public boolean
                    implies(ProtectionDomain domain, Permission permission) { return true; }
                });

                System.setSecurityManager(new SecurityManager());
            }
            smAvailable = true;
        } catch (UnsupportedOperationException e) {
            LOGGER.severe(
                "SECURITY WARNING: Janino Sandbox is NON-FUNCTIONAL on this JVM (Java 17+). "
                + "SecurityManager is deprecated/removed. "
                + "Compiled code has UNRESTRICTED access to all APIs including Runtime.exec(), "
                + "filesystem, network, and reflection. "
                + "Use process-level or container-level isolation for untrusted code."
            );
        } catch (Exception e) {
            LOGGER.severe(
                "SECURITY WARNING: Failed to initialize Sandbox SecurityManager: " + e.getMessage()
                + ". Sandbox provides NO protection."
            );
        }
        SECURITY_MANAGER_AVAILABLE = smAvailable;
    }

    private final AccessControlContext accessControlContext;

    /**
     * @param permissions Will be applied on later calls to {@link #confine(PrivilegedAction)} and {@link
     *                    #confine(PrivilegedExceptionAction)}
     * @throws SecurityException if the SecurityManager is not available (Java 17+)
     * @deprecated Use process-level isolation instead. This class is non-functional on Java 17+.
     */
    @Deprecated(since = "3.1.13", forRemoval = true)
    public
    Sandbox(PermissionCollection permissions) {
        if (!SECURITY_MANAGER_AVAILABLE) {
            throw new SecurityException(
                "Janino Sandbox is non-functional: SecurityManager is not available on this JVM "
                + "(Java " + System.getProperty("java.version") + "). "
                + "The Sandbox cannot restrict code execution. "
                + "Use process-level or container-level isolation instead."
            );
        }
        this.accessControlContext = new AccessControlContext(new ProtectionDomain[] {
            new ProtectionDomain(null, permissions)
        });
    }

    /**
     * Runs the given <var>action</var>, confined by the permissions configured through the {@link
     * #Sandbox(PermissionCollection) constructor}.
     *
     * @return The value returned by the <var>action</var>
     * @deprecated Use process-level isolation instead.
     */
    @Deprecated(since = "3.1.13", forRemoval = true)
    public <R> R
    confine(PrivilegedAction<R> action) {
        if (!SECURITY_MANAGER_AVAILABLE) {
            throw new SecurityException("Sandbox is non-functional on this JVM.");
        }
        return AccessController.doPrivileged(action, this.accessControlContext);
    }

    /**
     * @deprecated Use process-level isolation instead.
     */
    @Deprecated(since = "3.1.13", forRemoval = true)
    public <R> R
    confine(PrivilegedExceptionAction<R> action) throws Exception {
        if (!SECURITY_MANAGER_AVAILABLE) {
            throw new SecurityException("Sandbox is non-functional on this JVM.");
        }
        try {
            return AccessController.doPrivileged(action, this.accessControlContext);
        } catch (PrivilegedActionException pae) {
            throw pae.getException();
        }
    }

    /**
     * Returns whether the Sandbox can actually enforce restrictions on this JVM.
     * On Java 17+, this returns {@code false}.
     *
     * @return {@code true} if SecurityManager is available and the Sandbox can function
     */
    public static boolean isAvailable() {
        return SECURITY_MANAGER_AVAILABLE;
    }
}
