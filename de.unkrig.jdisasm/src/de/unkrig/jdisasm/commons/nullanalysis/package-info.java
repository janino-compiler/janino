
/*
 * JDISASM - A Java[TM] class file disassembler
 *
 * Copyright (c) 2016, Arno Unkrig
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
 * Annotations for ECLIPSE's "null analysis" feature.
 * <p>
 *   The following settings on the 'Java / Compiler / Errors/Warnings' preference page (or the 'Java Compiler /
 *   Errors/Warnings' project properties page) are recommended:
 * </p>
 * <pre>
 * <b>Null analysis</b>
 *   Null pointer access:                                                      [Error   ]   4.2+
 *   Potential null pointer access:                                            [Error   ]   4.2+
 *   Redundant null check:                                                     [Warning ]   4.2+
 *    [x] Include 'assert' in null analysis                                                 4.2+
 *    [x] Enable annotation-based null analysis                                             4.2+
 *        Violation of null specification:                                     [Warning ]   4.2+
 *        Conflict between null annotations and null inference:                [Warning ]   4.2+
 *        Unchecked conversion from non-annotated type to @NonNull type:       [Ignore  ]   4.2+
 *        Redundant null annotation:                                           [Warning ]   4.2+
 *        '@NonNull' parameter not annotated in overriding method:             [Warning ]   4.3+
 *        Missing '@NonNullByDefault' annotation on package                    [Warning ]   4.2+
 *        [ ] Use default annotations for null specifications (Configure...)                4.2+
 *        [ ] Inherit null annotations                                                      4.3+
 *        [x] Enable syntactic null analysis for fields                                     4.3+
 * </pre>
 * <p>
 *   Through the Configure... link, you should configure the following:
 * </p>
 * <pre>
 *    'Nullable' annotation:         [de.unkrig.jdisasm.commons.nullanalysis.Nullable         ]
 *    'NonNull' annotation:          [de.unkrig.jdisasm.commons.nullanalysis.NotNull          ]
 *    'NonNullByDefault' annotation: [de.unkrig.jdisasm.commons.nullanalysis.NotNullByDefault ]
 * </pre>
 * <p>
 *   (ECLIPSE Version: JUNO / 4.2, HELIOS / 4.3)
 * </p>
 */
// Don't put a NNBD here, otherwise ECLIPSE LUNA produces an internal compiler error.
// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=438449.
// Unfortunately, this produces a warning, and "@SuppressWarnings("null")" doesn't help for unknown reasons.
//@NotNullByDefault
package de.unkrig.jdisasm.commons.nullanalysis;
