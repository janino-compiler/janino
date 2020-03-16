
package test;

import static test.C1.*;
import static test.C2.*;

public
class StaticImports {
    
    public void
    execute() {
        System.out.println(testOverload((short) 1));
        System.out.println(testOverload((long) 2));
        System.out.println(testOverload((int) 3));
    }
}