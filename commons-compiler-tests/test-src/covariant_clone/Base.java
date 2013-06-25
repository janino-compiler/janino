
package covariant_clone;

public abstract
class Base implements Cloneable, CloneableData {

    /**
     * Clone this tuple, the new tuple will not share any buffers or data with the original
     * @return A copy of this Tuple
     */
    public Base
    clone() throws CloneNotSupportedException {
        //subclasses must implement
        throw new CloneNotSupportedException();
    }

    public Base
    other(long i, Object o) {
        throw new RuntimeException("Base.other() called");
    }
}
