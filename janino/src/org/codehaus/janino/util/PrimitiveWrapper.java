
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

package org.codehaus.janino.util;

/**
 * A helper class that wraps primitive values in their wrapper classes.
 */
public class PrimitiveWrapper {
    public static Byte      wrap(byte   v) { return new Byte(v);      }
    public static Short     wrap(short  v) { return new Short(v);     }
    public static Integer   wrap(int    v) { return new Integer(v);   }
    public static Long      wrap(long   v) { return new Long(v);      }
    public static Character wrap(char   v) { return new Character(v); }
    public static Float     wrap(float  v) { return new Float(v);     }
    public static Double    wrap(double v) { return new Double(v);    }
    public static Object    wrap(Object v) { return v;                }
}
