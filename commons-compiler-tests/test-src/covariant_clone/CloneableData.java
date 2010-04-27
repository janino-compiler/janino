package covariant_clone;

public interface CloneableData extends Cloneable {
    public CloneableData clone() throws CloneNotSupportedException;
};
