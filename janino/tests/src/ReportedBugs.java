
import java.io.*;

import org.codehaus.janino.Parser;
import org.codehaus.janino.Scanner;

import util.JaninoTestSuite;

import junit.framework.*;

public class ReportedBugs extends JaninoTestSuite {
    public ReportedBugs(String name) { super(name); }

    public static Test suite() {
        return new ReportedBugs();
    }

    public ReportedBugs() {
        super("Regression tests for reported bugs");

        section("Bug 48");
        asct(EXEC, "1", (
            "package demo;\n" +
            "public class Service {\n" +
            "    public void test() {\n" +
            "        Broken[] dummy = new Broken[5];\n" +
            "    }\n" +
            "    class Broken {\n" +
            "    }\n" +
            "}"
        ), "demo.Service");
        asct(EXEC, "2", (
            "package demo;\n" +
            "public class Service {\n" +
            "    public Broken[] test() {\n" +
            "        return null;\n" +
            "    }\n" +
            "}\n" +
            "class Broken {\n" +
            "}"
        ), "demo.Service");

        section(null);
        this.addTest(new TestCase("Bug 53") { protected void runTest() throws Exception {
            assertEquals(
                "new Foo(a, b, 7)",
                newStringParser("new Foo(a, b, 7)").parsePrimary().toString()
            );
            assertEquals(
                "new Foo(new Object() { ... })",
                newStringParser(
                    "new Foo(new Object() {\n" +
                    "    void meth(String s) {\n" +
                    "        System.out.println(s);\n" +
                    "    }\n" +
                    "})"
                ).parsePrimary().toString()
            );
        }});

        section("Bug 54");
        ast(TRUE, "0", (
            "String s = \"\";\n" +
            "try {\n" +
            "    {\n" +
            "        s += \"set1\";\n" +
            "    }\n" +
            "    {\n" +
            "        boolean tmp4 = false;\n" +
            "        if (tmp4) {\n" +
            "            {\n" +
            "                s += \"if true\";\n" +
            "                if (true) return false;\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "    {\n" +
            "        s += \"return\";\n" +
            "    }\n" +
            "} catch (Exception e) {\n" +
            "    s += \"exception\";\n" +
            "} finally {\n" +
            "    s +=\"finally\";\n" +
            "}\n" +
            "return \"set1returnfinally\".equals(s);"
        ));
        acbt(COOK, "1", (
            "void foo() {\n" +
            "    while (true) {\n" +
            "        if (true) {\n" +
            "            break;\n" +
            "        }\n" +
            "        return;\n" +
            "    }\n" +
            "}\n" +
            "void bar() {\n" +
            "    while (true) {\n" +
            "        {\n" +
            "            if (true) {\n" +
            "                break;\n" +
            "            }\n" +
            "        }\n" +
            "        return;\n" +
            "    }\n" +
            "}\n"
        ));
        acbt(COOK, "2", (
            "void baz1() {\n" +
            "    for (int i = 0; i < 100;) {\n" +
            "        {\n" +
            "            if (true) {\n" +
            "                break;\n" +
            "            }\n" +
            "        }\n" +
            "        i += 2;\n" +
            "    }\n" +
            "}\n"
        ));
        acbt(COOK, "3", (
            "void baz2() {\n" +
            "    for (int i = 0; i < 100; i++) {\n" +
            "        {\n" +
            "            if (true) {\n" +
            "                break;\n" +
            "            }\n" +
            "        }\n" +
            "        i += 2;\n" +
            "    }\n" +
            "}\n"
        ));
        acbt(COOK, "4", (
            "public void foo() throws Exception {\n" +
            "    for (int i = 0 ; true; i++) {\n" +
            "        break;\n" +
            "    }\n" +
            "}\n"
        ));
        acbt(COOK, "5", (
            "public void foo() throws Exception {\n" +
            "    for (int i = 0 ; true; i++) {\n" +
            "        if (true) { break; }\n" +
            "    }\n" +
            "}\n"
        ));
        acbt(COOK, "6", (
            "public void foo() throws Exception {\n" +
            "    {\n" +
            "        try {\n" +
            "            int i = 0;\n" +
            "            for (; true;) {\n" +
            "                try {\n" +
            "                    {\n" +
            "                        {\n" + // Invoke: break
            "                            if (true) { break; }\n" +
            "                        }\n" + // End Invoke: break
            "                    }\n" +
            "                    i++;\n" +
            "                } finally {}\n" +
            "                i++;\n" +
            "            }\n" +
            "            return;\n" +
            "        } finally {}\n" +
            "    }\n" +
            "}\n"
        ));
        ast(EXEC, "x", (
            "int c = 5;\n" +
            "if (c == 5) {\n" +
            "    if (true) return;\n" +
            "} else {\n" +
            "    return;\n" +
            "}\n" +
            "int b = 3;\n" // Physically unreachable, but logically reachable, hence not a compile error.
        ));

        section(null);
        asct(COOK, "Bug 55", (
            "class Junk {" + "\n" +
            "    double[][] x = { { 123.4 } };" + "\n" +
            "}"
        ), "Junk");

        section(null);
        ast(COOK, "Bug 56", (
            "int dummy3 = 3;\n" +
            "try {\n" +
            "    // 3 vars must be declared in try block\n" +
            "    int dummy5 = 5;\n" +
            "    int dummy4 = 4;\n" +
            "    boolean b = true;\n" +
            "\n" +
            "    while (b) {\n" +
            "        try {\n" +
            "            ++dummy5;\n" +                // Optional
            "            return;\n" +                  // <= Required
            "        } catch (Exception ex) {\n" +
            "            ++dummy5;\n" +
            "        }\n" +
            "    }\n" +
            "} finally {\n" +
            "    ++dummy3;\n" +
            "}\n"
        ));

        // Bug 57: See JLS2Tests 14.10.
        // Bug 60: See JLS2Tests 14.3/1.
    }

    /**
     * Create a {@link Parser} reading from a given {@link java.lang.String}.
     */
    private static Parser newStringParser(String s) throws Scanner.ScanException, IOException {
        return new Parser(new Scanner(null, new StringReader(s)));
    }
}
