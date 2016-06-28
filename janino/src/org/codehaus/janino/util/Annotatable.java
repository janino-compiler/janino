
/*
 * Janino - An embedded Java[TM] compiler
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

package org.codehaus.janino.util;

import java.util.Map;

import org.codehaus.janino.util.ClassFile.AnnotationsAttribute.ElementValue;
import org.codehaus.janino.util.ClassFile.ConstantUtf8Info;

/**
 * An object in a Java class file which can have annotations (classes, methods and fields).
 */
public
interface Annotatable {

    /**
     * @param runtimeVisible TODO
     * @return The annotations on this element; an empty array iff there are no annotations
     */
    ClassFile.AnnotationsAttribute.Annotation[]
    getAnnotations(boolean runtimeVisible);

    /**
     * Adds a "Runtime[In]visibleAnnotations" attribute to {@code this} object (if that annotation does not yet exist)
     * and adds an entry to it.
     *
     * @param elementValuePairs Maps "elemant_name_index" ({@link ConstantUtf8Info}) to "element_value", see JVMS8
     *                          4.7.16
     */
    void
    addAnnotationsAttributeEntry(
        boolean                  runtimeVisible,
        String                   fieldDescriptor,
        Map<Short, ElementValue> elementValuePairs
    );
}
