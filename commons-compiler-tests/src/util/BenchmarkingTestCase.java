package util;

import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.TestCase;
import junit.framework.TestResult;

public class BenchmarkingTestCase extends TestCase {
    private static final Logger LOGGER = Logger.getLogger(BenchmarkingTestCase.class.getName());

    private static final Level BENCHMARK_LEVEL = Level.INFO;

    public BenchmarkingTestCase() {
    }
    
    public BenchmarkingTestCase(String name) {
        super(name);
    }

    @Override
    public final void run(TestResult result) {
        long ns = System.nanoTime();
        super.run(result);
        LOGGER.log(BENCHMARK_LEVEL, (
            "'"
            + this.getName()
            + "' took "
            + (System.nanoTime() - ns)
            + " nanoseconds"
        ));
    }

}
