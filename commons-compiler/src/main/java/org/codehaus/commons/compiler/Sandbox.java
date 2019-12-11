
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
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;

import org.codehaus.commons.nullanalysis.NotNullByDefault;

/**
 * This class establishes a security manager that confines the permissions for code executed through classes that are
 * loaded through specific class loaders.
 * <p>
 *   "To execute through a class" means that the execution stack includes the class. E.g., if a method of class {@code
 *   A} invokes a method of class {@code B}, which then invokes a method of class {@code C}, and any of the three
 *   classes were loaded by a previously {@link #confine(ClassLoader, PermissionCollection) confined} class loader,
 *   then for all actions that are executed by class {@code C} the <i>intersection</i> of the three {@link
 *   PermissionCollection}s applies.
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
