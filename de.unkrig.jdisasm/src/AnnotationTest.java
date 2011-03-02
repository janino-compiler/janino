
public class AnnotationTest {

//    public @interface RequestForEnhancement {
//        int    id();        // Unique ID number associated with RFE
//        String synopsis();  // Synopsis of RFE
//        String engineer();  // Name of engineer who implemented RFE
//        String date();      // Date RFE was implemented
//    }
    // Annotation type declaration with defaults on some elements
    public @interface RequestForEnhancement {
        int    id();       // No default - must be specified in 
                                                            // each annotation
        String synopsis(); // No default - must be specified in 
                                                            // each annotation
        String engineer()  default "[unassigned]";
        String date()      default "[unimplemented]";
    }

    public @interface Preliminary { }

    public @interface Copyright {
        String value();
    }

    /**
     * Associates a list of endorsers with the annotated class.
     */
    public @interface Endorsers {
        String[] value();
    }

    public @interface Name {
        String first();
        String last();
    }

    public @interface Author {
        Name value();
    }

    public @interface Reviewer {
        Name value();
    }

    // Annotation type declaration with bounded wildcard to 
    // restrict Class annotation
    // The annotation type declaration below presumes the existence 
    // of this interface, which describes a formatter for Java 
    // programming language source code
    public interface Formatter { }

    // Designates a formatter to pretty-print the annotated class.
    public @interface PrettyPrinter {
        Class<? extends Formatter> value();
    }

    // Annotation type declaration with nested enum type declaration
    public @interface Quality {
        enum Level { BAD, INDIFFERENT, GOOD }

        Level value();
    }

    // USES

    // Single-element annotation
    @Copyright("2002 Yoyodyne Propulsion Systems, Inc., All rights reserved.")
    public static class OscillationOverthruster {

        OscillationOverthruster() {
            class Nested1 {}
        }
        
        void meth(@Copyright("foo") int x) {
            class Nested2 {
            }
            new Object(){};
        }
    }

    // Array-valued single-element annotation
    @Endorsers({"Children", "Unscrupulous dentists"})
    public static class Lollipop {  }

    // Single-element array-valued single-element annotation
    @Endorsers("Epicurus")
    public static class Pleasure {  }

    // Single-element complex annotation
    @Author(@Name(first = "Joe", last = "Hacker"))
    public static class BitTwiddle {  }

    // Normal annotation with default values
    @RequestForEnhancement(
        id       = 4561414,
        synopsis = "Balance the federal budget"
    )
    public static void balanceFederalBudget() {
        throw new UnsupportedOperationException("Not implemented");
    }

    // Single-element annotation with Class element restricted by bounded wildcard
    // The annotation presumes the existence of this class.
    public static class GorgeousFormatter implements Formatter {  }

    @PrettyPrinter(GorgeousFormatter.class)
    public static class Petunia { }
    // This annotation is illegal, as String is not a subtype of Formatter!!
//    @PrettyPrinter(String.class) public static class Begonia { ... }

    //Annotation using enum type declared inside the annotation type
    @Quality(Quality.Level.GOOD)
    public static class Karma {
    }

    public static <T extends Comparable<T>, EX extends Exception> T min(int ii, T... values) throws RuntimeException, EX {
        if (values.length == 0) throw new IllegalArgumentException();
        T min = values[0];
        for (int i = 1; i < values.length; ++i) {
            if (values[i].compareTo(min) < 0) {
                min = values[i];
            }
        }
        return min;
    }
}
