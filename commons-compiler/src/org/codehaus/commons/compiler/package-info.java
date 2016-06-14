
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote products derived from this software without
 *       specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * This package declares interfaces for the implementation of an {@link
 * org.codehaus.commons.compiler.IExpressionEvaluator}, an {@link org.codehaus.commons.compiler.IScriptEvaluator}, an
 * {@link org.codehaus.commons.compiler.IClassBodyEvaluator} and an {@link
 * org.codehaus.commons.compiler.ISimpleCompiler}. All of these adhere to the syntax of the Java &trade; programming
 * language.
 * <p>
 * There are (at least) two implementations of these interfaces available:
 * <ul>
 *   <li>
 *   <code>org.codehaus.janino</code>, available at <a href="http://janino.net">janino.net</a>: A lightweight,
 *   stand-alone implementation that implements Java 1.4 and half of Java 5. Runs on all JREs starting at version 1.3.
 *   <li>
 *   <code>org.codehaus.commons.compiler.jdk</code>, also available at <a href="http://janino.net">janino.net</a>:
 *   Uses the <code>javax.tools.JavaCompiler</code> API that is available since Java 1.6. Requires a JDK (not just a
 *   JRE).
 * </ul>
 *
 * Notice: Implementations may or may not be prone to "Java injection", i.e. it may or may not be possible to, e.g.,
 * break out of the "expression" scope of an {@link org.codehaus.commons.compiler.IExpressionEvaluator} by providing a
 * "bogus" expression text. The documentation of each implementation should state clearly whether or not it is prone to
 * Java injection.
 */
package org.codehaus.commons.compiler;
