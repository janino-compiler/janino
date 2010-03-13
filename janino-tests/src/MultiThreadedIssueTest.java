
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.codehaus.janino.*;

public class MultiThreadedIssueTest {
    public interface Calculator {
        double[] calc(int multiplier);
    }

    private static final AtomicBoolean running = new AtomicBoolean(true);
    private static final Random random = new Random(System.nanoTime());

    public static void main(String[] args) throws InterruptedException {
        Runnable runnable = new Runnable() {
            public void run() {
                final Calculator calculator = create(random.nextInt(100));
                calculator.calc((int)Math.random());
            }
        };

        List threads = new ArrayList();

        for (int i = 0; i < 100; i++) {
            final Thread thread = new Thread(runnable, "Thread_" + 1);
            threads.add(thread);
        }
        for (int i = 0; i < threads.size(); ++i) {
            ((Thread)threads.get(i)).start();
        }
        for (int i = 0; i < threads.size(); ++i) {
            ((Thread)threads.get(i)).join();
        }
        if (!running.get()) {
            System.exit(1);
        } else {
            System.exit(0);
        }
    }

    private static final AtomicLong version = new AtomicLong(0);

    public static Calculator create(int depth) {
        String javaCode = generateCode(depth);

        try {
            final String name = "Calculator_" + version.getAndAdd(1);
            return (Calculator) ClassBodyEvaluator.createFastClassBodyEvaluator(
                    new Scanner(name, new StringReader(javaCode)),
                    name,
                    null,
                    new Class[]{Calculator.class},
                    Thread.currentThread().getContextClassLoader()
            );
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
