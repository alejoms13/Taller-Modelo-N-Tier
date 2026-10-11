
package persistencia.json;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.temporal.TemporalAccessor;
import java.util.*;

public final class MiniJson {
    private MiniJson() {}

    public static Object parse(String s) {
        Parser p = new Parser(s);
        Object v = p.value();
        p.ws();
        if (p.i < s.length()) {
            throw new IllegalArgumentException("JSON sobrante");
        }
        return v;
    }

    public static String stringify(Object o) {
        StringBuilder b = new StringBuilder();
        write(o, b, new IdentityHashMap<>());
        return b.toString();
    }

    private static void write(Object o, StringBuilder b,
                              IdentityHashMap<Object, Boolean> visited) {
        if (o == null) {
            b.append("null");
        } else if (o instanceof String || o instanceof Character
                || o instanceof Enum<?> || o instanceof TemporalAccessor) {
            quote(o instanceof Enum<?> ? ((Enum<?>) o).name() : o.toString(), b);
        } else if (o instanceof Number) {
            Number n = (Number) o;
            if ((n instanceof Double && !Double.isFinite(n.doubleValue()))
                    || (n instanceof Float && !Float.isFinite(n.floatValue()))) {
                b.append("null");
            } else {
                b.append(n);
            }
        } else if (o instanceof Boolean) {
            b.append(o);
        } else if (o instanceof Map<?, ?>) {
            if (visited.put(o, true) != null) {
                throw new IllegalArgumentException("Referencia circular al serializar JSON");
            }
            b.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : ((Map<?, ?>) o).entrySet()) {
                if (!first) b.append(',');
                first = false;
                quote(String.valueOf(e.getKey()), b);
                b.append(':');
                write(e.getValue(), b, visited);
            }
            b.append('}');
            visited.remove(o);
        } else if (o instanceof Iterable<?>) {
            if (visited.put(o, true) != null) {
                throw new IllegalArgumentException("Referencia circular al serializar JSON");
            }
            b.append('[');
            boolean first = true;
            for (Object value : (Iterable<?>) o) {
                if (!first) b.append(',');
                first = false;
                write(value, b, visited);
            }
            b.append(']');
            visited.remove(o);
        } else if (o.getClass().isArray()) {
            if (visited.put(o, true) != null) {
                throw new IllegalArgumentException("Referencia circular al serializar JSON");
            }
            b.append('[');
            for (int i = 0; i < Array.getLength(o); i++) {
                if (i > 0) b.append(',');
                write(Array.get(o, i), b, visited);
            }
            b.append(']');
            visited.remove(o);
        } else {
            writeObject(o, b, visited);
        }
    }

    private static void writeObject(Object o, StringBuilder b,
                                    IdentityHashMap<Object, Boolean> visited) {
        if (visited.put(o, true) != null) {
            throw new IllegalArgumentException("Referencia circular al serializar JSON");
        }

        b.append('{');
        boolean first = true;
        Class<?> type = o.getClass();

        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                int modifiers = field.getModifiers();

                if (Modifier.isStatic(modifiers)
                        || Modifier.isTransient(modifiers)
                        || field.isSynthetic()) {
                    continue;
                }

                try {
                    if (!field.canAccess(o) && !field.trySetAccessible()) {
                        throw new IllegalArgumentException(
                                "No se puede acceder al campo: " + field.getName());
                    }

                    if (!first) b.append(',');
                    first = false;

                    quote(field.getName(), b);
                    b.append(':');
                    write(field.get(o), b, visited);
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException(
                            "No se pudo leer el campo: " + field.getName(), e);
                }
            }
            type = type.getSuperclass();
        }

        b.append('}');
        visited.remove(o);
    }

    private static void quote(String s, StringBuilder b) {
        b.append('"');
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"':  b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\b': b.append("\\b"); break;
                case '\f': b.append("\\f"); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                case '\t': b.append("\\t"); break;
                default:
                    if (c < 32) {
                        b.append(String.format("\\u%04x", (int) c));
                    } else {
                        b.append(c);
                    }
            }
        }
        b.append('"');
    }

    private static final class Parser {
        final String s;
        int i = 0;

        Parser(String s) {
            this.s = s;
        }

        void ws() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }

        Object value() {
            ws();
            if (i >= s.length()) throw err();

            char c = s.charAt(i);
            if (c == '{') return object();
            if (c == '[') return array();
            if (c == '"') return string();
            if (c == 't' && s.startsWith("true", i)) {
                i += 4;
                return true;
            }
            if (c == 'f' && s.startsWith("false", i)) {
                i += 5;
                return false;
            }
            if (c == 'n' && s.startsWith("null", i)) {
                i += 4;
                return null;
            }
            return number();
        }

        Map<String, Object> object() {
            i++;
            Map<String, Object> m = new LinkedHashMap<>();
            ws();
            if (take('}')) return m;

            while (true) {
                ws();
                String k = string();
                ws();
                need(':');
                m.put(k, value());
                ws();
                if (take('}')) return m;
                need(',');
            }
        }

        List<Object> array() {
            i++;
            List<Object> a = new ArrayList<>();
            ws();
            if (take(']')) return a;

            while (true) {
                a.add(value());
                ws();
                if (take(']')) return a;
                need(',');
            }
        }

        String string() {
            need('"');
            StringBuilder b = new StringBuilder();

            while (i < s.length()) {
                char c = s.charAt(i++);
                if (c == '"') return b.toString();

                if (c == '\\') {
                    if (i >= s.length()) throw err();
                    char e = s.charAt(i++);

                    switch (e) {
                        case '"': b.append('"'); break;
                        case '\\': b.append('\\'); break;
                        case '/': b.append('/'); break;
                        case 'b': b.append('\b'); break;
                        case 'f': b.append('\f'); break;
                        case 'n': b.append('\n'); break;
                        case 'r': b.append('\r'); break;
                        case 't': b.append('\t'); break;
                        case 'u':
                            if (i + 4 > s.length()) throw err();
                            b.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                            i += 4;
                            break;
                        default: throw err();
                    }
                } else {
                    if (c < 32) throw err();
                    b.append(c);
                }
            }
            throw err();
        }

        Number number() {
            int start = i;
            if (i < s.length() && s.charAt(i) == '-') i++;

            while (i < s.length() && Character.isDigit(s.charAt(i))) i++;

            if (i < s.length() && s.charAt(i) == '.') {
                i++;
                while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
            }

            if (i < s.length() && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
                i++;
                if (i < s.length() && (s.charAt(i) == '+' || s.charAt(i) == '-')) i++;
                while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
            }

            if (start == i) throw err();

            String n = s.substring(start, i);
            try {
                if (n.contains(".") || n.contains("e") || n.contains("E")) {
                    return Double.valueOf(n);
                }
                return Long.valueOf(n);
            } catch (NumberFormatException e) {
                throw err();
            }
        }

        boolean take(char c) {
            if (i < s.length() && s.charAt(i) == c) {
                i++;
                return true;
            }
            return false;
        }

        void need(char c) {
            ws();
            if (!take(c)) throw err();
        }

        IllegalArgumentException err() {
            return new IllegalArgumentException(
                    "JSON inválido cerca de posición " + i);
        }
    }
}
