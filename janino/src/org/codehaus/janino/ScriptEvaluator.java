
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2006, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *    1. Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.codehaus.janino;

import java.io.*;
import java.lang.reflect.*;

/**
 * A script evaluator that executes a script in Java<sup>TM</sup> bytecode.
 * <p>
 * The syntax of the script to compile is a sequence of import declarations followed by a
 * sequence of statements, as defined in the
 * <a href="http://java.sun.com/docs/books/jls/second_edition">Java Language Specification, 2nd
 * edition</a>, sections
 * <a href="http://java.sun.com/docs/books/jls/second_edition/html/packages.doc.html#70209">7.5</a>
 * and
 * <a href="http://java.sun.com/docs/books/jls/second_edition/html/statements.doc.html#101241">14</a>.
 * <p>
 * Example:
 * <pre>
 *   import java.text.*;
 * 
 *   System.out.println("HELLO");
 *   System.out.println(new DecimalFormat("####,###.##").format(a));
 * </pre>
 * (Notice that this expression refers to a parameter "a", as explained below.)
 * <p>
 * The script may complete abnormally, e.g. through a RETURN statement:
 * <pre>
 *   if (a == null) {
 *       System.out.println("Oops!");
 *       return;
 *   }
 * </pre>
 * Optionally, the script may be declared with a non-void return type. In this case, the last
 * statement of the script must be a RETURN statement (or a THROW statement), and all RETURN
 * statements in the script must return a value with the given type.
 * <p>
 * The script is compiled when the {@link ScriptEvaluator} object is instantiated. The script,
 * its return type, and its parameter names and types are specified at compile time.
 * <p>
 * The script evaluator is implemented by creating and compiling a temporary compilation unit
 * defining one class with one method the body of which consists of the statements of the
 * script.
 * <p>
 * After the {@link ScriptEvaluator} object is created, the script can be executed as often with
 * different parameter values (see {@link #evaluate(Object[])}). This execution is very fast,
 * compared to the compilation.
 * <p>
 * The more elaborate constructors of {@link ScriptEvaluator} also allow for the specification
 * of the name of the generated class, the class it extends, the interfaces it implements, the
 * name of the method that executes the script, the exceptions that this method is allowed
 * to throw, and the {@link ClassLoader} that is used to define the generated
 * class and to load classes referenced by the expression. This degree of flexibility is usually
 * not required; the most commonly used constructor is
 * {@link #ScriptEvaluator(String, Class, String[], Class[])}.
 */
public class ScriptEvaluator extends EvaluatorBase {
    private static final String DEFAULT_METHOD_NAME = "eval";
    private static final String DEFAULT_CLASS_NAME = "SC";

    /**
     * Parse a script from a {@link String} and compile it.
     * @see #ScriptEvaluator(Scanner, String, Class, Class[], boolean, Class, String, String[], Class[], Class[], ClassLoader)
     */
    public ScriptEvaluator(
        String   script
    ) throws CompileException, Parser.ParseException, Scanner.ScanException {
        super((ClassLoader) null);
        try {
            this.scanParseCompileLoad(
                new Scanner(null, new StringReader(script)), // scanner
                ScriptEvaluator.DEFAULT_CLASS_NAME,          // className
                (Class) null,                                // optionalExtendedType
                new Class[0],                                // implementedTypes
                true,                                        // staticMethod
                void.class,                                  // returnType
                ScriptEvaluator.DEFAULT_METHOD_NAME,         // methodName
                new String[0],                               // parameterNames
                new Class[0],                                // parameterTypes
                new Class[0]                                 // thrownExceptions
            );
        } catch (IOException ex) {
            throw new RuntimeException("SNO: StringReader throws IOException!?");
        }
    }

    /**
     * Parse a script from a {@link String} and compile it.
     * @see #ScriptEvaluator(Scanner, String, Class, Class[], boolean, Class, String, String[], Class[], Class[], ClassLoader)
     */
    public ScriptEvaluator(
        String   script,
        Class    returnType
    ) throws CompileException, Parser.ParseException, Scanner.ScanException {
        super((ClassLoader) null);
        try {
            this.scanParseCompileLoad(
                new Scanner(null, new StringReader(script)), // scanner
                ScriptEvaluator.DEFAULT_CLASS_NAME,          // className
                (Class) null,                                // optionalExtendedType
                new Class[0],                                // implementedTypes
                true,                                        // staticMethod
                returnType,                                  // returnType
                ScriptEvaluator.DEFAULT_METHOD_NAME,         // methodName
                new String[0],                               // parameterNames
                new Class[0],                                // parameterTypes
                new Class[0]                                 // thrownExceptions
            );
        } catch (IOException ex) {
            throw new RuntimeException("SNO: StringReader throws IOException!?");
        }
    }

    /**
     * Parse a script from a {@link String} and compile it.
     * @see #ScriptEvaluator(Scanner, String, Class, Class[], boolean, Class, String, String[], Class[], Class[], ClassLoader)
     */
    public ScriptEvaluator(
        String   script,
        Class    returnType,
        String[] parameterNames,
        Class[]  parameterTypes
    ) throws CompileException, Parser.ParseException, Scanner.ScanException {
        super((ClassLoader) null);
        try {
            this.scanParseCompileLoad(
                new Scanner(null, new StringReader(script)), // scanner
                ScriptEvaluator.DEFAULT_CLASS_NAME,          // className
                (Class) null,                                // optionalExtendedType
                new Class[0],                                // implementedTypes
                true,                                        // staticMethod
                returnType,                                  // returnType
                ScriptEvaluator.DEFAULT_METHOD_NAME,         // methodName
                parameterNames,                              // parameterNames
                parameterTypes,                              // parameterTypes
                new Class[0]                                 // thrownExceptions
            );
        } catch (IOException ex) {
            throw new RuntimeException("SNO: StringReader throws IOException!?");
        }
    }

    /**
     * Parse a script from a {@link String} and compile it.
     * @see #ScriptEvaluator(Scanner, String, Class, Class[], boolean, Class, String, String[], Class[], Class[], ClassLoader)
     */
    public ScriptEvaluator(
        String   script,
        Class    returnType,
        String[] parameterNames,
        Class[]  parameterTypes,
        Class[]  thrownExceptions
    ) throws CompileException, Parser.ParseException, Scanner.ScanException {
        super((ClassLoader) null);
        try {
            this.scanParseCompileLoad(
                new Scanner(null, new StringReader(script)), // scanner
                ScriptEvaluator.DEFAULT_CLASS_NAME,          // className
                (Class) null,                                // optionalExtendedType
                new Class[0],                                // implementedTypes
                true,                                        // staticMethod
                returnType,                                  // returnType
                ScriptEvaluator.DEFAULT_METHOD_NAME,         // methodName
                parameterNames,                              // parameterName
                parameterTypes,                              // parameterTypes
                thrownExceptions                             // thrownExceptions
            );
        } catch (IOException ex) {
            throw new RuntimeException("SNO: StringReader throws IOException!?");
        }
    }

    /**
     * Parse a script from an {@link InputStream} and compile it.
     * @see #ScriptEvaluator(Scanner, String, Class, Class[], boolean, Class, String, String[], Class[], Class[], ClassLoader)
     */
    public ScriptEvaluator(
        String      optionalFileName,
        InputStream is,
        Class       returnType,
        String[]    parameterNames,
        Class[]     parameterTypes,
        Class[]     thrownExceptions,
        ClassLoader optionalParentClassLoader // null = use current thread's context class loader
    ) throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        super(optionalParentClassLoader);
        this.scanParseCompileLoad(
            new Scanner(optionalFileName, is),   // scanner
            ScriptEvaluator.DEFAULT_CLASS_NAME,  // className
            (Class) null,                        // optionalExtendedType
            new Class[0],                        // implementedTypes
            true,                                // staticMethod
            returnType,                          // returnType
            ScriptEvaluator.DEFAULT_METHOD_NAME, // methodName
            parameterNames,                      // parameterName
            parameterTypes,                      // parameterTypes
            thrownExceptions                     // thrownExceptions
        );
    }

    /**
     * Parse a script from a {@link Reader} and compile it.
     * @see #ScriptEvaluator(Scanner, String, Class, Class[], boolean, Class, String, String[], Class[], Class[], ClassLoader)
     */
    public ScriptEvaluator(
        String      optionalFileName,
        Reader      reader,
        Class       returnType,
        String[]    parameterNames,
        Class[]     parameterTypes,
        Class[]     thrownExceptions,
        ClassLoader optionalParentClassLoader // null = use current thread's context class loader
    ) throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        super(optionalParentClassLoader);
        this.scanParseCompileLoad(
            new Scanner(optionalFileName, reader), // scanner
            ScriptEvaluator.DEFAULT_CLASS_NAME,    // className
            (Class) null,                          // optionalExtendedType
            new Class[0],                          // implementedTypes
            true,                                  // staticMethod
            returnType,                            // returnType
            ScriptEvaluator.DEFAULT_METHOD_NAME,   // methodName
            parameterNames,                        // parameterNames
            parameterTypes,                        // parameterTypes
            thrownExceptions                       // thrownExceptions
        );
    }

    /**
     * Parse a script from a sequence of {@link Scanner.Token}s delivered by the given
     * {@link Scanner} object and compile it.
     * 
     * @see #ScriptEvaluator(Scanner, String, Class, Class[], boolean, Class, String, String[], Class[], Class[], ClassLoader)
     */
    public ScriptEvaluator(
        Scanner     scanner,
        Class       returnType,
        String[]    parameterNames,
        Class[]     parameterTypes,
        Class[]     thrownExceptions,
        ClassLoader optionalParentClassLoader // null = use current thread's context class loader
    ) throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        super(optionalParentClassLoader);
        this.scanParseCompileLoad(
            scanner,                             // scanner
            ScriptEvaluator.DEFAULT_CLASS_NAME,  // className
            (Class) null,                        // optionalExtendedType
            new Class[0],                        // implementedTypes
            true,                                // staticMethod
            returnType,                          // returnType
            ScriptEvaluator.DEFAULT_METHOD_NAME, // methodName
            parameterNames,                      // parameterNames
            parameterTypes,                      // parameterTypes
            thrownExceptions                     // thrownExceptions
        );
    }

    /**
     * Parse a script from a sequence of {@link Scanner.Token}s delivered by the given
     * {@link Scanner} object and compile it.
     * 
     * @see #ScriptEvaluator(Scanner, String, Class, Class[], boolean, Class, String, String[], Class[], Class[], ClassLoader)
     */
    public ScriptEvaluator(
        Scanner     scanner,
        Class       optionalExtendedType,
        Class[]     implementedTypes,
        Class       returnType,
        String[]    parameterNames,
        Class[]     parameterTypes,
        Class[]     thrownExceptions,
        ClassLoader optionalParentClassLoader // null = use current thread's context class loader
    ) throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        super(optionalParentClassLoader);
        this.scanParseCompileLoad(
            scanner,                             // scanner
            ScriptEvaluator.DEFAULT_CLASS_NAME,  // className
            optionalExtendedType,                // optionalExtendedType
            implementedTypes,                    // implementedTypes
            true,                                // staticMethod
            returnType,                          // returnType
            ScriptEvaluator.DEFAULT_METHOD_NAME, // methodName
            parameterNames,                      // parameterName
            parameterTypes,                      // parameterTypes
            thrownExceptions                     // thrownExceptions
        );
    }

    /**
     * Construct a script evaluator that processes the given script
     * with the given return type, parameter names and types. A script is a
     * sequence of valid Java<sup>TM</sup> statements.
     * <p>
     * If the return type of the script is {@link Void#TYPE}, then all RETURN
     * statements must have no value, and the script need not be concluded by a RETURN
     * statement.
     * <p>
     * <code>parameterNames</code> and <code>parameterTypes</code> must have the same length.
     * <p>
     * The <code>optionalParentClassLoader</code> serves two purposes:
     * <ul>
     *   <li>It is used to look for classes referenced by the script.
     *   <li>It is used to load the generated Java<sup>TM</sup> class
     *   into the JVM; directly if it is a subclass of {@link ByteArrayClassLoader},
     *   or by creation of a temporary {@link ByteArrayClassLoader} if not.
     * </ul>
     * A <code>null</code> <code>optionalParentClassLoader</code> means to use the current
     * thread's context class loader.
     * <p>
     * A number of constructors exist that provide useful default values for the various
     * parameters, or parse their script from a {@link String}, an {@link InputStream}
     * or a {@link Reader} instead of a {@link Scanner}.
     *
     * @param scanner Source of tokens to parse
     * @param className Name of the temporary class (uncritical)
     * @param optionalExtendedType Superclass of the temporary class or <code>null</code>
     * @param implementedTypes The interfaces that the the generated object implements (all methods must be implemented by the <code>optionalExtendedType</code>)
     * @param returnType The return type of the temporary method that implements the script, e.g. {@link Double#TYPE} or {@link Void#TYPE}
     * @param methodName The name of the temporary method (uncritical)
     * @param parameterNames The names of the script parameters, e.g. "i" and "j".
     * @param parameterTypes The types of the script parameters, e.g. {@link Integer#TYPE} or {@link Double#TYPE}.
     * @param thrownExceptions The exceptions that the script is allowed to throw, e.g. {@link IOException}<code>.class</code>.
     * @param optionalParentClassLoader Loads referenced classes
     */
    public ScriptEvaluator(
        Scanner     scanner,
        String      className,
        Class       optionalExtendedType,
        Class[]     implementedTypes,
        boolean     staticMethod,
        Class       returnType,
        String      methodName,
        String[]    parameterNames,
        Class[]     parameterTypes,
        Class[]     thrownExceptions,
        ClassLoader optionalParentClassLoader // null = use current thread's context class loader
    ) throws Scanner.ScanException, Parser.ParseException, CompileException, IOException {
        super(optionalParentClassLoader);
        this.scanParseCompileLoad(
            scanner,              // scanner
            className,            // className
            optionalExtendedType, // optionalExtendedType
            implementedTypes,     // implementedTypes
            staticMethod,         // staticMethod
            returnType,           // returnType
            methodName,           // methodName
            parameterNames,       // parameterNames
            parameterTypes,       // parameterTypes
            thrownExceptions      // thrownExceptions
        );
    }

    /**
     * Workhorse method used by all {@link ScriptEvaluator} constructors.
     */
    private void scanParseCompileLoad(
        Scanner  scanner,
        String   className,
        Class    optionalExtendedType,
        Class[]  implementedTypes,
        boolean  staticMethod,
        Class    returnType,
        String   methodName,
        String[] parameterNames,
        Class[]  parameterTypes,
        Class[]  thrownExceptions
    ) throws Scanner.ScanException, Parser.ParseException, CompileException, IOException {
        if (parameterNames.length != parameterTypes.length) throw new RuntimeException("Lengths of \"parameterNames\" and \"parameterTypes\" do not match");

        // Create a temporary compilation unit.
        Java.CompilationUnit compilationUnit = new Java.CompilationUnit(scanner.peek().getLocation().getFileName());

        // Parse import declarations.
        this.parseImportDeclarations(compilationUnit, scanner);

        // Create class, method and block.
        Java.Block block = this.addClassMethodBlockDeclaration(
            scanner.peek().getLocation(),           // location
            compilationUnit,                        // compilationUnit
            className,                              // className
            optionalExtendedType, implementedTypes, // optionalExtendedType, implementedTypes
            staticMethod,                           // staticMethod
            returnType, methodName,                 // returnType, methodName
            parameterNames, parameterTypes,         // parameterNames, parameterTypes
            thrownExceptions                        // thrownExceptions
        );
        
        // Parse block statements.
        Parser parser = new Parser(scanner);
        while (!scanner.peek().isEOF()) {
            block.addStatement(parser.parseBlockStatement(block));
        }

        // Compile and load it.
        Class c;
        try {
            c = this.compileAndLoad(
                compilationUnit,                                    // compilationUnit
                DebuggingInformation.DEFAULT_DEBUGGING_INFORMATION, // debuggingInformation
                className                                           // className
            );
        } catch (ClassNotFoundException e) {
            throw new RuntimeException();
        }

        // Find script method by name.
        try {
            this.method = c.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex.toString());
        }
        if (this.method == null) throw new RuntimeException("Method \"" + methodName + "\" not found");
    }

    /**
     * Simplified version of
     * {@link #createFastScriptEvaluator(Scanner, Class, String[], ClassLoader)}.
     * 
     * @param script Contains the sequence of script tokens
     * @param interfaceToImplement Must declare exactly the one method that defines the expression's signature
     * @param parameterNames The expression references the parameters through these names
     * @return an object that implements the given interface and extends the <code>optionalExtendedType</code>
     */
    public static Object createFastScriptEvaluator(
        String   script,
        Class    interfaceToImplement,
        String[] parameterNames
    ) throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        try {
            return ScriptEvaluator.createFastScriptEvaluator(
                new Scanner(null, new StringReader(script)), // scanner
                interfaceToImplement,                        // interfaceToImplement
                parameterNames,                              // parameterNames
                null                                         // optionalParentClassLoader
            );
        } catch (IOException ex) {
            throw new RuntimeException("SNO: StringReader throws IOException!?");
        }
    }

    /**
     * If the parameter and return types of the expression are known at compile time,
     * then a "fast" script evaluator can be instantiated through this method.
     * <p>
     * Script evaluation is faster than through {@link #evaluate(Object[])}, because
     * it is not done through reflection but through direct method invocation.
     * <p>
     * Example:
     * <pre>
     * public interface Foo {
     *     int bar(int a, int b);
     * }
     * ...
     * Foo f = (Foo) ScriptEvaluator.createFastScriptEvaluator(
     *     new Scanner(null, new StringReader("return a + b;")),
     *     Foo.class,
     *     new String[] { "a", "b" },
     *     (ClassLoader) null          // Use current thread's context class loader
     * );
     * System.out.println("1 + 2 = " + f.bar(1, 2));
     * </pre>
     * Notice: The <code>interfaceToImplement</code> must either be declared <code>public</code>,
     * or with package scope in the root package (i.e. "no" package).
     * 
     * @param scanner Source of script tokens
     * @param interfaceToImplement Must declare exactly one method
     * @param parameterNames
     * @param optionalParentClassLoader
     * @return an object that implements the given interface
     */
    public static Object createFastScriptEvaluator(
        Scanner     scanner,
        Class       interfaceToImplement,
        String[]    parameterNames,
        ClassLoader optionalParentClassLoader
    ) throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        return ScriptEvaluator.createFastScriptEvaluator(
            scanner,                            // scanner
            ScriptEvaluator.DEFAULT_CLASS_NAME, // className
            null,                               // optionalExtendedType
            interfaceToImplement,               // interfaceToImplement
            parameterNames,                     // parameterNames
            optionalParentClassLoader           // optionalParentClassLoader
        );
    }

    /**
     * Like {@link #createFastScriptEvaluator(Scanner, Class, String[], ClassLoader)},
     * but gives you more control over the generated class (rarely needed in practice).
     * <p> 
     * Notice: The <code>interfaceToImplement</code> must either be declared <code>public</code>,
     * or with package scope in the same package as <code>className</code>.
     * 
     * @param scanner Source of script tokens
     * @param className Name of generated class
     * @param optionalExtendedType Class to extend
     * @param interfaceToImplement Must declare exactly the one method that defines the expression's signature
     * @param parameterNames The expression references the parameters through these names
     * @param optionalParentClassLoader Loads referenced classes
     * @return an object that implements the given interface and extends the <code>optionalExtendedType</code>
     */
    public static Object createFastScriptEvaluator(
        Scanner     scanner,
        String      className,
        Class       optionalExtendedType,
        Class       interfaceToImplement,
        String[]    parameterNames,
        ClassLoader optionalParentClassLoader
    ) throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        if (!interfaceToImplement.isInterface()) throw new RuntimeException("\"" + interfaceToImplement + "\" is not an interface");

        Method[] methods = interfaceToImplement.getDeclaredMethods();
        if (methods.length != 1) throw new RuntimeException("Interface \"" + interfaceToImplement + "\" must declare exactly one method");
        Method methodToImplement = methods[0];

        Class c = new ScriptEvaluator(
            scanner,                               // scanner
            className,                             // className
            optionalExtendedType,                  // optionalExtendedType
            new Class[] { interfaceToImplement },  // implementedTypes
            false,                                 // staticMethod
            methodToImplement.getReturnType(),     // returnType
            methodToImplement.getName(),           // methodName
            parameterNames,                        // parameterNames
            methodToImplement.getParameterTypes(), // parameterTypes
            methodToImplement.getExceptionTypes(), // thrownExceptions
            optionalParentClassLoader              // optionalParentClassLoader
        ).getMethod().getDeclaringClass();
        try {
            return c.newInstance();
        } catch (InstantiationException e) {
            // SNO - Declared class is always non-abstract.
            throw new RuntimeException(e.toString());
        } catch (IllegalAccessException e) {
            // SNO - interface methods are always PUBLIC.
            throw new RuntimeException(e.toString());
        }
    }

    /**
     * Evaluates a script with concrete parameter values.
     *
     * <p>
     *   Each parameter value must have the same type as specified through
     *   the "parameterTypes" parameter of
     *   {@link org.codehaus.janino.ScriptEvaluator#ScriptEvaluator(String,
     *   Class, String[], Class[])}.
     * </p>
     * <p>
     *   Parameters of primitive type must passed with their wrapper class
     *   objects.
     * </p>
     * <p>
     *   The object returned has the class specified through the "returnType"
     *   parameter of
     *   {@link org.codehaus.janino.ScriptEvaluator#ScriptEvaluator(String,
     *   Class, String[], Class[])}.
     * </p>
     * <p>
     *   This method is thread-safe.
     * </p>
     *
     * @param parameterValues The concrete parameter values.
     */
    public Object evaluate(Object[] parameterValues) throws InvocationTargetException {
        try {
            return this.method.invoke(null, parameterValues);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex.toString());
        }
    }

    /**
     * If, for any reason, somebody needs the
     * {@link Method} object...
     *
     */
    public Method getMethod() {
        return this.method;
    }

    private Method method;
}
