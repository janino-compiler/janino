
package covariant_clone;

public
class Middle extends Base {

    public Base
    cloneWithOutArguments() {
        try {
            return clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Clone not supported on class: " + getClass().getName(), e);
        }
    }

    public Middle
    cloneWithArguments() {
        return other(1, null);
    }

    public Middle
    other(long i, Object o) {
        throw new RuntimeException("Middle() called");
    }
}
