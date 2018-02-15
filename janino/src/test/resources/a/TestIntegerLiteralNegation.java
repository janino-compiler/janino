package a;

// Issue #41 : Invalid integer literal "9223372036854775808L"
public
class TestIntegerLiteralNegation {
    void foo() {
        int x = -(-2147483648);
        long y = -(-9223372036854775808L);
    }
}

