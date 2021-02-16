package gov.nasa.worldwind.util;

/** replaces: junit jupiter ThrowingConsumer */
public interface SafeConsumer<X> {
    void accept(X x) throws Throwable;
}