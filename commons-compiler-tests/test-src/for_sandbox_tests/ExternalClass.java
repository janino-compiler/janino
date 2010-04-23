
package for_sandbox_tests;

public class ExternalClass extends BaseOfExternalClass implements InterfaceOfExternalClass {
    public final OtherExternalClass x = new OtherExternalClass();

    public static int m1() { return OtherExternalClass.m1(); }
    public OtherExternalClass m2() { return null; }
    public void m2(OtherExternalClass fc) { }
}
