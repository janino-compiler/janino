
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2006, Arno Unkrig
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

package org.codehaus.janino.tools;

import java.io.*;
import java.text.*;
import java.util.*;

/**
 * Example for object allocation statistics:
 * 
 *     java -Xrunhprof:heap=sites,monitor=n,cutoff=0,depth=4 MyClass
 */
public class HprofScrubber {
    private static class Site {
        public final int allocatedBytes;
        public final int allocatedObjects;
        public final int traceNumber;
        public final String className;
        public Site(int allocatedBytes, int allocatedObjects, int traceNumber, String className) {
            this.allocatedBytes   = allocatedBytes;
            this.allocatedObjects = allocatedObjects;
            this.traceNumber      = traceNumber;
            this.className        = className;
        }
    }

    private static class Sample {
        public final int count;
        public final int traceNumber;
        public Sample(int count, int traceNumber) {
            this.count       = count;
            this.traceNumber = traceNumber;
        }
    }

    public static void main(String[] args) throws Exception {
        String fileName = args.length == 0 ? "java.hprof.txt" : args[0];

        BufferedReader br = new BufferedReader(new FileReader(fileName));
        try {
            Map traces = new HashMap(); // Integer number => String[] stackFrames
            List sites = new ArrayList(); // Site
            List samples = new ArrayList(); // Sample

            String s = br.readLine();
            while (s != null) {
                if (s.startsWith("SITES BEGIN")) {
                    br.readLine();
                    br.readLine();
                    for (;;) {
                        s = br.readLine();
                        if (s.startsWith("SITES END")) break;
                        StringTokenizer st = new StringTokenizer(s);
                        st.nextToken(); // rank
                        st.nextToken(); // percent self
                        st.nextToken(); // percent accum
                        st.nextToken(); // live bytes
                        st.nextToken(); // live objects
                        sites.add(new Site(
                            Integer.parseInt(st.nextToken()), // allocatedBytes
                            Integer.parseInt(st.nextToken()), // allocatedObjects
                            Integer.parseInt(st.nextToken()), // traceNumber
                            st.nextToken()                    // className
                        ));
                    }
                } else
                if (s.startsWith("TRACE ") && s.endsWith(":")) {
                    int traceNumber = Integer.parseInt(s.substring(6, s.length() - 1));
                    List l = new ArrayList();
                    for (;;) {
                        s = br.readLine();
                        if (!s.startsWith("\t")) break;
                        l.add(s.substring(1));
                    }
                    traces.put(
                        new Integer(traceNumber),
                        l.toArray(new String[l.size()])
                    );
                } else
                if (s.startsWith("CPU SAMPLES BEGIN")) {
                    br.readLine();
                    for (;;) {
                        s = br.readLine();
                        if (s.startsWith("CPU SAMPLES END")) break;
                        StringTokenizer st = new StringTokenizer(s);
                        st.nextToken(); // rank
                        st.nextToken(); // percent self
                        st.nextToken(); // percent accum
                        int count = Integer.parseInt(st.nextToken());
                        if (count == 0) continue;
                        int trace = Integer.parseInt(st.nextToken());
                        samples.add(new Sample(count, trace));
                    }
                } else {
                    s = br.readLine();
                }
            }

            HprofScrubber.dumpSites((Site[]) sites.toArray(new Site[sites.size()]), traces);

            HprofScrubber.dumpSamples((Sample[]) samples.toArray(new Sample[samples.size()]), traces);

        } finally {
            try { br.close(); } catch (IOException e) {}
        }
    }

    private static void dumpSites(Site[] ss, Map traces) {
        Arrays.sort(ss, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((Site) o2).allocatedBytes - ((Site) o1).allocatedBytes;
            }
        });
    
        int totalAllocatedBytes = 0, totalAllocatedObjects = 0;
        for (int i = 0; i < ss.length; ++i) {
            Site site = ss[i];
            totalAllocatedBytes += site.allocatedBytes;
            totalAllocatedObjects += site.allocatedObjects;
        }
    
        System.out.println("          percent          alloc'ed");
        System.out.println("rank   self  accum      bytes  objects  class name");
        System.out.println("Total:              " + totalAllocatedBytes + "  " + totalAllocatedObjects);
    
        double accumulatedPercentage = 0.0;
        MessageFormat mf = new MessageFormat("{0,number,00000} {1,number,00.00}% {2,number,00.00}% {3,number,000000000} {4,number,000000000} {5}");
        for (int i = 0; i < ss.length; ++i) {
            Site site = ss[i];
            double selfPercentage = 100.0 * ((double) site.allocatedBytes / (double) totalAllocatedBytes);
            accumulatedPercentage += selfPercentage;
    //      System.out.println((i + 1) + "      " + selfPercentage + "% " + accumulatedPercentage + "%    " + site.allocatedBytes + " " + site.allocatedObjects + " "  + site.className);
            System.out.println(mf.format(
                new Object[] {
                    new Integer(i + 1),
                    new Double(selfPercentage),
                    new Double(accumulatedPercentage),
                    new Integer(site.allocatedBytes),
                    new Integer(site.allocatedObjects),
                    site.className
                },
                new StringBuffer(),
                new FieldPosition(0)
            ));
            String[] stackFrames = (String[]) traces.get(new Integer(site.traceNumber));
            if (stackFrames != null) {
                for (int j = 0; j < stackFrames.length; ++j) {
                    System.out.println("                           " + stackFrames[j]);
                }
            }
        }
    }

    private static void dumpSamples(Sample[] ss, Map traces) {
        int totalCount = 0;
        for (int i = 0; i < ss.length; ++i) totalCount += ss[i].count;

        System.out.println("          percent");
        System.out.println("rank   self  accum      count");
        System.out.println("Total:              " + totalCount);

        double accumulatedPercentage = 0.0;
        MessageFormat mf = new MessageFormat("{0,number,00000} {1,number,00.00}% {2,number,00.00}% {3,number,000000000}");
        for (int i = 0; i < ss.length; ++i) {
            Sample sample = ss[i];
            double selfPercentage = 100.0 * ((double) sample.count / (double) totalCount);
            accumulatedPercentage += selfPercentage;
//          System.out.println((i + 1) + "      " + selfPercentage + "% " + accumulatedPercentage + "%    " + site.allocatedBytes + " " + site.allocatedObjects + " "  + site.className);
            System.out.println(mf.format(
                new Object[] {
                    new Integer(i + 1),
                    new Double(selfPercentage),
                    new Double(accumulatedPercentage),
                    new Integer(sample.count)
                },
                new StringBuffer(),
                new FieldPosition(0)
            ));
            String[] stackFrames = (String[]) traces.get(new Integer(sample.traceNumber));
            if (stackFrames != null) {
                for (int j = 0; j < stackFrames.length; ++j) {
                    System.out.println("                           " + stackFrames[j]);
                }
            }
        }
    }
}
