
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

package org.codehaus.janino;

import org.codehaus.janino.util.enum.*;

public class DebuggingInformation extends EnumeratorSet {
    public static final DebuggingInformation SOURCE = new DebuggingInformation(1);
    public static final DebuggingInformation LINES  = new DebuggingInformation(2);
    public static final DebuggingInformation VARS   = new DebuggingInformation(4);

    public static final DebuggingInformation NONE   = new DebuggingInformation(0);
    public static final DebuggingInformation ALL    = new DebuggingInformation(7);

    public DebuggingInformation(String s) throws EnumeratorFormatException {
        super(s);
    }

    public DebuggingInformation add(DebuggingInformation that) {
        return new DebuggingInformation(super.add(that));
    }

    public DebuggingInformation remove(DebuggingInformation that) {
        return new DebuggingInformation(super.remove(that));
    }

    private DebuggingInformation(int values) { super(values); }
}
