
package de.unkrig.io.charstream;

import java.io.IOException;

public class UnexpectedCharacterException extends IOException {
   
    private static final long serialVersionUID = 1L;

    public UnexpectedCharacterException() {
    }
    
    public UnexpectedCharacterException(String message) {
        super(message);
    }
}
