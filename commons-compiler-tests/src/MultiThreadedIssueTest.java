
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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.TestCase;

import org.codehaus.commons.compiler.IClassBodyEvaluator;
import org.codehaus.commons.compiler.ICompilerFactory;

public class MultiThreadedIssueTest extends TestCase {

    public interface Calculator {
        double[] calc(int multiplier);
    }

    private final ICompilerFactory compilerFactory;

    private static final AtomicBoolean running = new AtomicBoolean(true);
    private static final Random random = new Random(System.nanoTime());

    public MultiThreadedIssueTest(ICompilerFactory compilerFactory) {
        super("MultiThreadedIssueTest");
        this.compilerFactory = compilerFactory;
    }

    @Override
    protected void runTest() throws Throwable {
        Runnable runnable = new Runnable() {
            public void run() {
                final Calculator calculator = create(random.nextInt(100));
                calculator.calc((int) (100.0 * Math.random()));
            }
        };

        List<Thread> threads = new ArrayList<Thread>();

        for (int i = 0; i < 100; i++) {
            final Thread thread = new Thread(runnable, "Thread_" + 1);
            threads.add(thread);
        }
        for (int i = 0; i < threads.size(); ++i) {
            ((Thread) threads.get(i)).start();
        }
        for (int i = 0; i < threads.size(); ++i) {
            ((Thread) threads.get(i)).join();
        }
        if (!running.get()) {
            System.exit(1);
        } else {
            System.exit(0);
        }
    }

    private static final AtomicLong version = new AtomicLong(0);

    public Calculator create(int depth) {
        String javaCode = generateCode(depth);

        try {
            final String name = "Calculator_" + version.getAndAdd(1);
            IClassBodyEvaluator cbe = compilerFactory.newClassBodyEvaluator();
            cbe.setClassName(name);
            cbe.setImplementedInterfaces(new Class[] { Calculator.class });
            return (Calculator) cbe.createInstance(new StringReader(javaCode));
        } catch (Exception e) {
            e.printStackTrace();
            running.set(false);
            throw new RuntimeException(e);
        }
    }

    public static String generateCode(int depth) {
        StringBuilder sb = new StringBuilder();

        sb.append("public double[] calc(int multiplier) {\n");
        sb.append("  double[] result = new double[").append(depth).append("];\n");

        for (int i = 0; i < depth; i++) {
            sb.append("  result[").append(i).append("] = ").append(Math.random()).append(" * multiplier;\n");
        }

        sb.append("  return result;\n");
        sb.append("}\n");

        return sb.toString();
    }

}
