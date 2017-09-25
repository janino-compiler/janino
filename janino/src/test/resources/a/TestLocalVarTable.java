// SUPPRESS CHECKSTYLE .:9999

package a;


// Issue #5 : ClassFormatError: Invalid start_pc 65459 in LocalVariableTable in class file
public
class TestLocalVarTable {
    void foo() {
        double a = 1.0;
        if (false)
        {
            double b;
            b = 1.0;
        }
    }
}

