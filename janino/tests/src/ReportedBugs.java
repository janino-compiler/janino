
import java.io.*;
import java.lang.reflect.*;

import org.codehaus.janino.SimpleCompiler;

import junit.framework.*;

public class ReportedBugs extends TestCase {
    public ReportedBugs(String name) { super(name); }

    public static Test suite() {
        TestSuite suite = new TestSuite("Regression tests for reported bugs");
        suite.addTest(new ReportedBugs("testBug48_1"));
        suite.addTest(new ReportedBugs("testBug48_2"));
        return suite;
    }

    public void testBug48_1() throws Exception {
        Class c = new SimpleCompiler(null, new StringReader(
            "package demo;\n" +
            "public class Service {\n" +
            "    public void test() {\n" +
            "        Broken[] dummy = new Broken[5];\n" +
            "    }\n" +
            "    class Broken {\n" +
            "    }\n" +
            "}"
        )).getClassLoader().loadClass("demo.Service");
        Method m = c.getMethod("test", new Class[0]);
        m.invoke(c.newInstance(), new Object[0]);
    }

    public void testBug48_2() throws Exception {
        Class c = new SimpleCompiler(null, new StringReader(
            "package demo;\n" +
            "public class Service {\n" +
            "    public Broken[] test() {\n" +
            "        return null;\n" +
            "    }\n" +
            "}\n" +
            "class Broken {\n" +
            "}"
        )).getClassLoader().loadClass("demo.Service");
        Method m = c.getMethod("test", new Class[0]);
        m.invoke(c.newInstance(), new Object[0]);
    }
}
