
package other_package;

/**
 * Helper class for {@link JLS2Tests} -- used to define accessibility tests.
 */
public class Foo {
    public      Foo(int i) {}
    /*package*/ Foo(String s) {}
    protected   Foo(boolean b) {}
    private     Foo(char c) {}

    private static void     privateStaticMethod() {}
    private void            privateMethod() {}
    /*package*/ static void packageStaticMethod() {}
    /*package*/ void        packageMethod() {}
    protected static void   protectedStaticMethod() {}
    protected void          protectedMethod() {}
    public static void      publicStaticMethod() {}
    public void             publicMethod() {}

    private static class         PrivateStaticMemberClass { }
    private class                PrivateMemberClass { }
    /*package*/ static class     PackageStaticMemberClass { }
    /*package*/ class            PackageMemberClass { }
    protected static class       ProtectedStaticMemberClass { }
    protected class              ProtectedMemberClass { }
    public static class          PublicStaticMemberClass { }
    public class                 PublicMemberClass { }
    public abstract static class PublicAbstractStaticMemberClass { }
    public abstract class        PublicAbstractMemberClass { }

    private static interface     PrivateStaticMemberInterface { }
    private interface            PrivateMemberInterface { }
    /*package*/ static interface PackageStaticMemberInterface { }
    /*package*/ interface        PackageMemberInterface { }
    protected static interface   ProtectedStaticMemberInterface { }
    protected interface          ProtectedMemberInterface { }
    public static interface      PublicStaticMemberInterface { }
    public interface             PublicMemberInterface { }

    void useMembersToSuppressWarnings() {
        new Foo('c');
        privateStaticMethod();
        privateMethod();
        new PrivateStaticMemberClass();
        new PrivateMemberClass();
        new PrivateStaticMemberInterface() { };
        new PrivateMemberInterface() { };
    }
}

/*package*/ class     PackageClass { }
/*package*/ interface PackageInterface { }
