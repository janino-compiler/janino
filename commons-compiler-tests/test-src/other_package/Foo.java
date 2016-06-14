
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote products derived from this software without
 *       specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

// CHECKSTYLE Javadoc:OFF

package other_package; // SUPPRESS CHECKSTYLE PackageName

/** Helper class for {@link JlsTests} -- used to define accessibility tests. */
public
class Foo {
    public      Foo(int i)     {}
    /*package*/ Foo(String s)  {} // SUPPRESS CHECKSTYLE WrapAndIndent
    protected   Foo(boolean b) {}
    private     Foo(char c)    {}

    private static void   privateStaticMethod()   {}
    private void          privateMethod()         {}
    static void           packageStaticMethod()   {}
    void                  packageMethod()         {}
    protected static void protectedStaticMethod() {}
    protected void        protectedMethod()       {}
    public static void    publicStaticMethod()    {}
    public void           publicMethod()          {}

    private static class         PrivateStaticMemberClass {}
    private class                PrivateMemberClass {}
    static class                 PackageStaticMemberClass {}
    class                        PackageMemberClass {}
    protected static class       ProtectedStaticMemberClass {}
    protected class              ProtectedMemberClass {}
    public static class          PublicStaticMemberClass {}
    public class                 PublicMemberClass {}
    public abstract static class PublicAbstractStaticMemberClass {}
    public abstract class        PublicAbstractMemberClass {}

    private static interface     PrivateStaticMemberInterface {} // SUPPRESS CHECKSTYLE RedundantModifier
    private interface            PrivateMemberInterface {}
    static interface             PackageStaticMemberInterface {} // SUPPRESS CHECKSTYLE RedundantModifier
    interface                    PackageMemberInterface {}
    protected static interface   ProtectedStaticMemberInterface {} // SUPPRESS CHECKSTYLE RedundantModifier
    protected interface          ProtectedMemberInterface {}
    public static interface      PublicStaticMemberInterface {} // SUPPRESS CHECKSTYLE RedundantModifier
    public interface             PublicMemberInterface {}

    void
    useMembersToSuppressWarnings() {
        new Foo('c');
        privateStaticMethod();
        this.privateMethod();
        new PrivateStaticMemberClass();
        new PrivateMemberClass();
        new PrivateStaticMemberInterface() {};
        new PrivateMemberInterface() {};
    }
}

class     PackageClass {}
interface PackageInterface {}
