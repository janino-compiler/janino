package util;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.commons.compiler.CompilerFactoryFactory;
import org.codehaus.commons.compiler.ICompilerFactory;
import org.junit.runners.Parameterized.Parameters;

public final
class TestUtil {
    /**
     * Return the available compiler factories in a format suitable for JUnit {@link Parameters}
     */
    public static List<Object[]>
    getCompilerFactoriesForParameters() throws Exception {
        ArrayList<Object[]> f = new ArrayList<Object[]>();
        for (ICompilerFactory fact : CompilerFactoryFactory.getAllCompilerFactories()) {
            f.add(new Object[] { fact });
        }
        if (f.isEmpty()) {
            throw new RuntimeException("Could not find any Compiler Factories on the classpath");
        }
        return f;
    }

    private TestUtil() {}
}
