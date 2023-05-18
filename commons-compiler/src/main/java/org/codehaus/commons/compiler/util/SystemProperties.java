
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2020 Arno Unkrig. All rights reserved.
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

package org.codehaus.commons.compiler.util;

import org.codehaus.commons.nullanalysis.Nullable;

public final
class SystemProperties {

    private SystemProperties() {}

    /**
     * @return Whether the class property is set and its value equals, ignoring case, {@code "true"}
     * @see    #getClassProperty(Class, String, String)
     */
    public static boolean
    getBooleanClassProperty(Class<?> targetClass, String classPropertyName) {
        return SystemProperties.getBooleanClassProperty(targetClass, classPropertyName, false);
    }

    /**
     * @return Whether the value of the class property its value equals, ignoring case, {@code "true"}, or, if that
     *         class property is not set, <var>defaultValue</var>
     * @see    #getClassProperty(Class, String, String)
     */
    public static boolean
    getBooleanClassProperty(Class<?> targetClass, String classPropertyName, boolean defaultValue) {
        String s = SystemProperties.getClassProperty(targetClass, classPropertyName);
        return s != null ? Boolean.parseBoolean(s) : defaultValue;
    }

    /**
     * @return                       The value of the class property, converted to integer, or, if that class property
     *                               is not set, <var>defaultValue</var>
     * @throws NumberFormatException The value of the class property could be parsed as an integer
     * @see    #getClassProperty(Class, String, String)
     * @see    Integer#parseInt(String)
     */
    public static int
    getIntegerClassProperty(Class<?> targetClass, String classPropertyName, int defaultValue) {
        String s = SystemProperties.getClassProperty(targetClass, classPropertyName);
        return s != null ? Integer.parseInt(s) : defaultValue;
    }

    /**
     * @return The value of the class property, or, if that class property is not set, {@code null}
     * @see    #getClassProperty(Class, String, String)
     */
    @Nullable public static String
    getClassProperty(Class<?> targetClass, String classPropertyName) {
        return SystemProperties.getClassProperty(targetClass, classPropertyName, null);
    }

    /**
     * Gets the value of a "class property".
     * <p>
     *   A class property is configured by a set of system properties (decreasing priority):
     * </p>
     * <ul>
     *   <li><var>fully-qualified-name-of-targetClass</var>{@code .}<var>classPropertyName</var></li>
     *   <li><var>simple-name-of-targetClass</var>{@code .}<var>classPropertyName</var></li>
     * </ul>
     *
     * @return The value of the class property, or, if that class property is not set, <var>defaultValue</var>
     */
    @Nullable public static String
    getClassProperty(Class<?> targetClass, String classPropertyName, @Nullable String defaultValue) {

        String result;

        try {
            result = System.getProperty(targetClass.getName() + "." + classPropertyName);
            if (result != null) return result;
        } catch (RuntimeException e) {
            // Do nothing, e.g. if blocked by security policy
        }

        try {
            result = System.getProperty(targetClass.getSimpleName() + "." + classPropertyName);
            if (result != null) return result;
        } catch (RuntimeException e) {
            // Do nothing, e.g. if blocked by security policy
        }

        return defaultValue;
    }
}
