
package for_sandbox_tests;

public class OverridesWithDifferingVisibility {
    public static void test(Object o) { }
    private static void test(Object[] arr) { }

    // squish a compiler warning
    static { test(new Object[] { }); }
}
