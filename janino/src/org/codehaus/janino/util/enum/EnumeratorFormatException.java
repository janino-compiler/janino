
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright 2004 Arno Unkrig
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.janino.util.enum;

/**
 * Represents a problem related to parsing {@link org.codehaus.janino.util.enum.Enumerator}s
 * and {@link org.codehaus.janino.util.enum.EnumeratorSet}s, in analogy with
 * {@link EnumeratorFormatException}.
 */
public class EnumeratorFormatException extends Exception {
    public EnumeratorFormatException()               {}
    public EnumeratorFormatException(String message) { super(message); }
}
