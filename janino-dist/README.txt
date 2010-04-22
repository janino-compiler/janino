Quick start for JANINO users:

(1) Put "commons-compiler.jar" and "janino.jar" on your class path.

(2) If you're using an IDE like ECLIPSE, you can optionally have
    "commons-compiler-src.zip" as the source attachment of
    "commons-compiler.jar", and "janino-src.zip" as the source attachment of
    "janino.jar". That'll get you tooltip JAVADOC and source level debugging
    into the JANINO libraries.

(3) Use one of the features, e.g. the "expression evaluator", in your program:

        import org.codehaus.janino.*;

        ExpressionEvaluator ee = new ExpressionEvaluator();
        ee.cook("3 + 4");
        System.out.println(ee.evaluate(null));

(4) Compile, run, ... be happy!
