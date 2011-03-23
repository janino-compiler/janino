package covariant_clone;

public interface CloneableData extends Cloneable {
    CloneableData clone() throws CloneNotSupportedException;
};
