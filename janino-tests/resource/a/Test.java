package a;
// ISSUE: https://github.com/codehaus/janino/issues/4
public class Test {
    public class Parent {
        void foo () { System.out.println("from parent"); }
    }

    public class Child extends Parent {
        void foo () { System.out.println("from child"); }

        void bar() {
            Child.super.foo();
        }
    }
}
