
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2007, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *    1. Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package util;

import java.util.*;

import junit.framework.*;

/**
 * A {@link junit.framework.TestSuite} that adds support for easy creation of suite hierarchies. By default,
 * it behaves exactly like the {@link junit.framework.TestSuite}, but if you call {@link #section(int, String)},
 * {@link #section(String)} or {@link #section(String, char)}, sub-suites are created and nested, and immediately
 * following calls to {@link #addTest(Test)} and {@link #addTestSuite(Class)} add to the most recently defined
 * sub-suite instead of this {@link junit.framework.TestSuite}.
 */
public class StructuredTestSuite extends TestSuite {
    private final Stack_ subSuites = new ArrayListStack();

    public StructuredTestSuite() {
    }
    public StructuredTestSuite(String name) {
        super(name);
    }

    /**
     * Add a {@link Test} to the most recently created sub-suite (if any, otherwise to this
     * {@link TestSuite}).
     */
    public void addTest(Test test) {
        if (this.subSuites.isEmpty()) {
            super.addTest(test);
        } else
        {
            ((TestSuite) this.subSuites.peek()).addTest(test);
        }
    }

    /**
     * Add a {@link TestSuite} to the most recently created sub-suite (if any, otherwise to this
     * {@link TestSuite}).
     */
    public void addTestSuite(Class testClass) {
        if (this.subSuites.isEmpty()) {
            super.addTestSuite(testClass);
        } else
        {
            ((TestSuite) this.subSuites.peek()).addTestSuite(testClass);
        }
    }

    /**
     * Create a sub-suite with the given <code>name</code>, and insert it one the given hierarchy
     * level. Levels start with "1".
     * <p>
     * As a special case, <code>level</code> zero resets to this {@link TestSuite}, and the
     * <code>name</code> parameter is ignored.
     * <p>
     * Notice: If you indent by more than one level (i.e. <code>level > previous_level + 1</code>),
     * then unnamed intermediate sub-suites are inserted as needed (probably not what you want,
     * because these sub-suites appear with name "junit.framework.TestSuite").
     * 
     * @param name the name of the sub-suite (or <code>null</code> for an unnamed sub-suite)
     */
    public void section(int level, String name) {

        // Special case: Reset to top level suite.
        if (level <= 0) {
            this.subSuites.clear();
            return;
        }

        // Abjust the sub-suite stack.
        {
            int delta = level - this.subSuites.size();

            // Create, add and push intermediate sub-suites as necessary.
            for (; delta > 1; --delta) {
                TestSuite intermediateSubSection = new TestSuite(); // Appears with name "junit.framework.TestSuite"
                this.addTest(intermediateSubSection);
                this.subSuites.push(intermediateSubSection);
            }

            // Pop sub-suites as necessary.
            for (; delta <= 0; ++delta) this.subSuites.pop();
        }

        // Create, add and push a new test suite.
        TestSuite ts = new TestSuite(name);
        this.addTest(ts);
        this.subSuites.push(ts);
    }

    /**
     * Convenience method for {@link #section(int, String)}: The level is automatically determined
     * by counting the period characters in the first word of the <code>name</code> and adding
     * one.
     * <p>
     * Examples:
     * <dl>
     *   <dt>3 Functional requirements
     *   <dd>Level 1 sub-suite "3 Functional requirements"
     *   <dt>15.9.1 Determining the class being Instantiated
     *   <dd>Level 3 sub-suite
     *   <dt>Appendix_A Charsets
     *   <dd>Level 1 sub-suite
     *   <dt>Appendix_A.3 EBCDIC
     *   <dd>Level 2 sub-suite
     * </dl>
     *
     * @see #section(int, String)
     */
    public void section(String name) {
        this.section(name, '.');
    }

    /**
     * Like {@link #section(String)}, but with a custom separator character.
     */
    public void section(String name, char separator) {
        if (name == null) {
            this.section(0, null);
        } else
        {
            int level = 1;
            for (int i = 0; i < name.length(); ++i) {
                char c = name.charAt(i);
                if (Character.isWhitespace(c)) break;
                if (c == separator) ++level;
            }
            this.section(level, name);
        }
    }
}

/**
 * "java.util" lacks a "Stack" interface.
 * {@link java.util.Stack} is a bad alternative, because it is synchronized and hence slow.
 */
interface Stack_ { // Avoid name conflict with "java.util.Stack".
    public void push(Object o);
    public Object pop();
    public Object peek();
    public boolean isEmpty();
    public int size();
    public void clear();
}

/** A simple implementation of the {@link Stack_} interface. */
class ArrayListStack extends ArrayList implements Stack_ {
    public void   push(Object o) { this.add(o); }
    public Object peek()         { return this.get(this.size() - 1); }
    public Object pop()          { return this.remove(this.size() - 1); }
}
