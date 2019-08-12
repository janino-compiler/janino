
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2018 Arno Unkrig. All rights reserved.
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

package org.codehaus.janino;

/**
 * The compilation of {@link Compiler}, {@link JavaSourceIClassLoader}, {@link SimpleCompiler} and their subclasses
 * can be configured with these options.
 *
 * @see Compiler#options()
 * @see JavaSourceIClassLoader#options()
 * @see SimpleCompiler#options()
 */
public
enum JaninoOption {

    /**
     * Contrary to the JLS, disallow access to {@code private} members of types enclosed by the the accessing code, or
     * enclosed by the accessing code, or enclosed by the same top-level type as the accessing code.
     */
    PRIVATE_MEMBERS_OF_ENCLOSING_AND_ENCLOSED_TYPES_INACCESSIBLE,

    /**
     * Contrary to the JLS, allow <em>any</em> expression as a resource in a TRY-with-resources statement.
     */
    EXPRESSIONS_IN_TRY_WITH_RESOURCES_ALLOWED,
}
