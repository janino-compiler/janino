
package de.unkrig.jdisasm;

import java.io.IOException;

public class ClassFileFormatException extends IOException {

    public ClassFileFormatException(String message) {
        super(message);
    }

    public ClassFileFormatException(String message, Throwable cause) {
        super(message);
        this.initCause(cause);
    }
}
