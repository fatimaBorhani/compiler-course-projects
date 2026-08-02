package main.visitor;

import main.ast.types.Type;
import main.ast.types.PrimitiveType;
import main.ast.types.UserDefinedType;

/**
 * Small helper for working with {@link Type} nodes during semantic analysis.
 */
public final class TypeUtils {

    private TypeUtils() {}

    /** Human readable name of a type, used inside error messages. */
    public static String name(Type t) {
        if (t == null) {
            return "void";
        }
        if (t instanceof PrimitiveType) {
            return ((PrimitiveType) t).getStr();
        }
        if (t instanceof UserDefinedType) {
            return ((UserDefinedType) t).getStr();
        }
        String s = t.getStr();
        return s == null ? "unknown" : s;
    }

    /** True if the two types are exactly the same (no implicit conversions). */
    public static boolean same(Type a, Type b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a instanceof PrimitiveType && b instanceof PrimitiveType) {
            return ((PrimitiveType) a).getPrimitive() == ((PrimitiveType) b).getPrimitive();
        }
        if (a instanceof UserDefinedType && b instanceof UserDefinedType) {
            return name(a).equals(name(b));
        }
        return false;
    }

    public static boolean isPrimitive(Type t, PrimitiveType.Primitive p) {
        return t instanceof PrimitiveType && ((PrimitiveType) t).getPrimitive() == p;
    }

    public static boolean isBool(Type t) {
        return isPrimitive(t, PrimitiveType.Primitive.BOOL);
    }

    public static boolean isNumeric(Type t) {
        if (!(t instanceof PrimitiveType)) {
            return false;
        }
        switch (((PrimitiveType) t).getPrimitive()) {
            case INT:
            case FLOAT:
            case DOUBLE:
            case CHAR:
                return true;
            default:
                return false;
        }
    }
}
