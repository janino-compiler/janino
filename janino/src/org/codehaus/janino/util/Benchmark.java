
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

import java.util.Stack;

/**
 * Implements a scheme for benchmarking, i.e. for determining and/or reporting the time elapsed
 * between the beginning and the end of an activity.
 * <p>
 * The measurement is done by invoking {@link #begin()} and later calling {@link #end()} whichs
 * returns the time elapsed since the call to {@link #begin()}.
 * <p>
 * Notice that calls to {@link #begin()} and {@link #end()} can be nested, and each call to
 * {@link #end()} refers to the matching {@link #begin()} call. To ensure that all calls match,
 * the preferred way to write a benchmark is
 * <pre>
 * ...
 * Benchmark b = new Benchmark();
 * ...
 * b.begin();
 * try {
 *     ....
 * } finally {
 *     long ms = b.end();
 * }
 * </pre>
 * This code layout also makes it visually easy to write correct pairs of {@link #begin()} /
 * {@link #end()} pairs.
 * <p>
 * The pair {@link #beginReporting()} and {@link #endReporting()} do basically the same, but
 * report the benchmarking information through an internal {@link Reporter} object. The default
 * {@link Reporter} prints its messages by <code>System.out.println()</code>.
 * <p>
 * Reporting is only enabled if the Benchmark object was created through {@link #Benchmark(boolean)}
 * with a <code>true</code> argument.
 */
public class Benchmark {
    private final Stack beginTimes = new Stack(); // Long

    public Benchmark() {
        this.reportingEnabled = false;
        this.reporter         = null;
    }

    /**
     * @see Benchmark
     */
    public void begin() {
        this.beginTimes.push(new Long(System.currentTimeMillis()));
    }

    /**
     * @see Benchmark
     */
    public long end() {
        return System.currentTimeMillis() - ((Long) this.beginTimes.pop()).longValue();
    }

    // Reporting-related methods and fields.

    /**
     * Set up a {@link Benchmark} with a default {@link Reporter} that reports to
     * <code>System.out</code>.
     */
    public Benchmark(boolean reportingEnabled) {
        this.reportingEnabled = reportingEnabled;
        this.reporter         = new Reporter() {
            public void report(String message) { System.out.println(message); }
        };
    }

    /**
     * Set up a {@link Benchmark} with a custom {@link Reporter}.
     */
    public Benchmark(
        boolean reportingEnabled,
        Reporter reporter
    ) {
        this.reportingEnabled = reportingEnabled;
        this.reporter         = reporter;
    }

    private final boolean  reportingEnabled;
    private final Reporter reporter;

    /**
     * Interface used to report messages.
     */
    public interface Reporter {
        void report(String message);
    }

    /**
     * Begin a benchmark (see {@link #begin()}) and report the fact.
     */
    public void beginReporting() {
        if (!this.reportingEnabled) return;

        this.reportIndented("Beginning...");
        this.begin();
    }

    /**
     * Begin a benchmark (see {@link #begin()}) and report the fact.
     */
    public void beginReporting(String message) {
        if (!this.reportingEnabled) return;
        this.reportIndented(message + "...");
        this.begin();
    }

    /**
     * End a benchmark (see {@link #end()}) and report the fact.
     */
    public void endReporting() {
        if (!this.reportingEnabled) return;
        this.reportIndented("... took " + this.end() + " ms");
    }

    /**
     * End a benchmark (see {@link #begin()}) and report the fact.
     */
    public void endReporting(String message) {
        if (!this.reportingEnabled) return;
        this.reportIndented("... took " + this.end() + " ms: " + message);
    }

    /**
     * Report the given message.
     */
    public void report(String message) {
        if (!this.reportingEnabled) return;
        this.reportIndented(message);
    }

    /**
     * Report the <code>title</code>, a colon, a space, and the pretty-printed
     * {@link Object}.
     * @param optionalTitle
     * @param o
     */
    public void report(String optionalTitle, Object o) {
        if (!this.reportingEnabled) return;

        String prefix = optionalTitle == null ? "" : (
            optionalTitle
            + ": "
            + (optionalTitle.length() < Benchmark.PAD.length() ? Benchmark.PAD.substring(optionalTitle.length()) : "") 
        );

        if (o == null) {
            this.reportIndented(prefix + "(undefined)");
        } else
        if (o.getClass().isArray()) {
            Object[] oa = (Object[]) o;
            if (oa.length == 0) {
                this.reportIndented(prefix + "(empty)");
            } else
            if (oa.length == 1) {
                this.reportIndented(prefix + oa[0].toString());
            } else {
                this.reportIndented(optionalTitle == null ? "Array:" : optionalTitle + ':');
                this.begin();
                try {
                    for (int i = 0; i < oa.length; ++i) this.report(null, oa[i]);
                } finally {
                    this.end();
                }
            }
        } else
        {
            this.reportIndented(prefix + o.toString());
        }
    }
    private static final String PAD = "                       ";

    /**
     * Report a message through {@link #reporter}, indent by N spaces where N is the current
     * benchmark stack depth.
     */
    private void reportIndented(String message) {
        StringBuffer sb = new StringBuffer();
        for (int i = this.beginTimes.size(); i > 0; --i) sb.append("  ");
        sb.append(message);
        this.reporter.report(sb.toString());
    }
}
