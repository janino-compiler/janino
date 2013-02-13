
package de.unkrig.jdisasm;

import java.io.IOException;

public
class ClassFileFormatException extends IOException {

    private static final long serialVersionUID = 1L;

    public
    ClassFileFormatException(String message) {
        super(message);
    }

    public
    ClassFileFormatException(String message, Throwable cause) {
        super(message);
        this.initCause(cause);
    }
}
