import org.codehaus.janino.CompileException;
import org.codehaus.janino.ExpressionEvaluator;
import org.codehaus.janino.SimpleCompiler;

import for_sandbox_tests.ExternalClass;
import for_sandbox_tests.OtherExternalClass;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


public class SandboxTests extends TestCase {

    public SandboxTests(String name) {
        super(name);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite("Sandbox tests");
        suite.addTest(new SandboxTests("testForbiddenClass"));
        suite.addTest(new SandboxTests("testAuxiliaryClass"));
        suite.addTest(new SandboxTests("testExternalBaseClass"));
        return suite;
    }

    public void testForbiddenClass() throws Exception {
        
        // Invoke method of forbidden external class.
        try {
            ExpressionEvaluator ee = new ExpressionEvaluator();
            ee.setParentClassLoader(SimpleCompiler.BOOT_CLASS_LOADER);
            ee.cook("for_sandbox_tests.ExternalClass.m1()");
            fail("Should have thrown a CompileException");
        } catch (CompileException ex) {
            ;
        }
    }

    public void testAuxiliaryClass() throws Exception {

        // Invoke method of allowed external class.
        ExpressionEvaluator ee = new ExpressionEvaluator();
        ee.setParentClassLoader(null, new Class[] { ExternalClass.class });
        ee.cook("for_sandbox_tests.ExternalClass.m1()");
        assertEquals(7, ((Integer) ee.evaluate(new Object[0])).intValue());
    }

    public void testExternalBaseClass() throws Exception {

        // Invoke method of base class.
        ExpressionEvaluator ee = new ExpressionEvaluator();
        ee.setParentClassLoader(SimpleCompiler.BOOT_CLASS_LOADER, new Class[] { OtherExternalClass.class });
        ee.setExtendedType(ExternalClass.class);
        ee.cook("m1()");
        assertEquals(7, ((Integer) ee.evaluate(new Object[0])).intValue());
    }
}

