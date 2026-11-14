package frcsim.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Bagimliliksiz kucuk JSON okuyucu/yazici. Sayilar Double olarak okunur. */
public final class Json {
  private Json() {}

  public static Object parse(String text) {
    Parser p = new Parser(text);
    p.skipWs();
    Object v = p.readValue(0);
    p.skipWs();
    if (p.pos != text.length()) throw p.error("Beklenmeyen karakter");
    return v;
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> parseObject(String text) {
    Object o = parse(text);
    if (!(o instanceof Map)) throw new IllegalArgumentException("JSON nesnesi {...} bekleniyordu");
    return (Map<String, Object>) o;
  }

  // ---- tipli okuyucular (m null olabilir) ----

  public static double num(Map<String, Object> m, String key, double def) {
    Object v = m == null ? null : m.get(key);
    return v instanceof Number ? ((Number) v).doubleValue() : def;
  }

  public static int integer(Map<String, Object> m, String key, int def) {
    return (int) Math.round(num(m, key, def));
  }

  public static String str(Map<String, Object> m, String key, String def) {
    Object v = m == null ? null : m.get(key);
    return v instanceof String ? (String) v : def;
  }

  public static boolean bool(Map<String, Object> m, String key, boolean def) {
    Object v = m == null ? null : m.get(key);
    return v instanceof Boolean ? (Boolean) v : def;
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> obj(Map<String, Object> m, String key) {
    Object v = m == null ? null : m.get(key);
    return v instanceof Map ? (Map<String, Object>) v : null;
  }

  @SuppressWarnings("unchecked")
  public static List<Object> list(Map<String, Object> m, String key) {
    Object v = m == null ? null : m.get(key);
    return v instanceof List ? (List<Object>) v : null;
  }

  public static int[] intArray(Map<String, Object> m, String key, int[] def) {
    List<Object> l = list(m, key);
    if (l == null) return def;
    int[] r = new int[l.size()];
    for (int i = 0; i < r.length; i++) {
      if (!(l.get(i) instanceof Number)) {
        throw new IllegalArgumentException("'" + key + "' listesi yalnizca sayilardan olusmali");
      }
      r[i] = (int) Math.round(((Number) l.get(i)).doubleValue());
    }
    return r;
  }

  // ---- yazici ----

  public static String stringify(Object value) {
    StringBuilder sb = new StringBuilder();
    write(sb, value);
    return sb.toString();
  }

  private static void write(StringBuilder sb, Object v) {
    if (v == null) {
      sb.append("null");
    } else if (v instanceof String) {
      quote(sb, (String) v);
    } else if (v instanceof Boolean) {
      sb.append(v.toString());
    } else if (v instanceof Number) {
      number(sb, ((Number) v).doubleValue());
    } else if (v instanceof Map) {
      sb.append('{');
      boolean first = true;
      for (Map.Entry<?, ?> e : ((Map<?, ?>) v).entrySet()) {
        if (!first) sb.append(',');
        first = false;
        quote(sb, String.valueOf(e.getKey()));
        sb.append(':');
        write(sb, e.getValue());
      }
      sb.append('}');
    } else if (v instanceof Iterable) {
      sb.append('[');
      boolean first = true;
      for (Object o : (Iterable<?>) v) {
        if (!first) sb.append(',');
        first = false;
        write(sb, o);
      }
      sb.append(']');
    } else if (v instanceof double[]) {
      double[] a = (double[]) v;
      sb.append('[');
      for (int i = 0; i < a.length; i++) {
        if (i > 0) sb.append(',');
        number(sb, a[i]);
      }
      sb.append(']');
    } else if (v instanceof int[]) {
      int[] a = (int[]) v;
      sb.append('[');
      for (int i = 0; i < a.length; i++) {
        if (i > 0) sb.append(',');
        sb.append(a[i]);
      }
      sb.append(']');
    } else if (v instanceof Object[]) {
      List<Object> l = new ArrayList<>();
      for (Object o : (Object[]) v) l.add(o);
      write(sb, l);
    } else {
      quote(sb, v.toString());
    }
  }

  private static void number(StringBuilder sb, double d) {
    if (Double.isNaN(d) || Double.isInfinite(d)) {
      sb.append("null");
      return;
    }
    double r = Math.rint(d * 1e6) / 1e6;
    if (r == Math.rint(r) && Math.abs(r) < 1e15) {
      sb.append((long) r);
    } else {
      sb.append(r);
    }
  }

  private static void quote(StringBuilder sb, String s) {
    sb.append('"');
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '"': sb.append("\\\""); break;
        case '\\': sb.append("\\\\"); break;
        case '\n': sb.append("\\n"); break;
        case '\r': sb.append("\\r"); break;
        case '\t': sb.append("\\t"); break;
        case '\b': sb.append("\\b"); break;
        case '\f': sb.append("\\f"); break;
        default:
          if (c < 0x20) {
            sb.append(String.format("\\u%04x", (int) c));
          } else {
            sb.append(c);
          }
      }
    }
    sb.append('"');
  }

  // ---- ayristirici ----

  private static final class Parser {
    private static final int MAX_DEPTH = 64;
    final String s;
    int pos;

    Parser(String s) {
      this.s = s;
    }

    IllegalArgumentException error(String msg) {
      return new IllegalArgumentException("JSON hatasi (konum " + pos + "): " + msg);
    }

    void skipWs() {
      while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
    }

    Object readValue(int depth) {
      if (depth > MAX_DEPTH) throw error("Cok derin ic ice yapi");
      if (pos >= s.length()) throw error("Beklenmeyen son");
      char c = s.charAt(pos);
      switch (c) {
        case '{': return readObject(depth);
        case '[': return readArray(depth);
        case '"': return readString();
        case 't': expect("true"); return Boolean.TRUE;
        case 'f': expect("false"); return Boolean.FALSE;
        case 'n': expect("null"); return null;
        default: return readNumber();
      }
    }

    void expect(String lit) {
      if (!s.startsWith(lit, pos)) throw error(lit + " bekleniyordu");
      pos += lit.length();
    }

    Map<String, Object> readObject(int depth) {
      pos++;
      Map<String, Object> m = new LinkedHashMap<>();
      skipWs();
      if (pos < s.length() && s.charAt(pos) == '}') {
        pos++;
        return m;
      }
      while (true) {
        skipWs();
        if (pos >= s.length() || s.charAt(pos) != '"') throw error("Anahtar bekleniyordu");
        String k = readString();
        skipWs();
        if (pos >= s.length() || s.charAt(pos) != ':') throw error("':' bekleniyordu");
        pos++;
        skipWs();
        m.put(k, readValue(depth + 1));
        skipWs();
        if (pos >= s.length()) throw error("'}' bekleniyordu");
        char c = s.charAt(pos++);
        if (c == '}') return m;
        if (c != ',') throw error("',' veya '}' bekleniyordu");
      }
    }

    List<Object> readArray(int depth) {
      pos++;
      List<Object> l = new ArrayList<>();
      skipWs();
      if (pos < s.length() && s.charAt(pos) == ']') {
        pos++;
        return l;
      }
      while (true) {
        skipWs();
        l.add(readValue(depth + 1));
        skipWs();
        if (pos >= s.length()) throw error("']' bekleniyordu");
        char c = s.charAt(pos++);
        if (c == ']') return l;
        if (c != ',') throw error("',' veya ']' bekleniyordu");
      }
    }

    String readString() {
      pos++;
      StringBuilder sb = new StringBuilder();
      while (true) {
        if (pos >= s.length()) throw error("Kapanmamis metin");
        char c = s.charAt(pos++);
        if (c == '"') return sb.toString();
        if (c == '\\') {
          if (pos >= s.length()) throw error("Kapanmamis metin");
          char e = s.charAt(pos++);
          switch (e) {
            case 'n': sb.append('\n'); break;
            case 'r': sb.append('\r'); break;
            case 't': sb.append('\t'); break;
            case 'b': sb.append('\b'); break;
            case 'f': sb.append('\f'); break;
            case 'u':
              if (pos + 4 > s.length()) throw error("Gecersiz \\u dizisi");
              try {
                sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
              } catch (NumberFormatException ex) {
                throw error("Gecersiz \\u dizisi");
              }
              pos += 4;
              break;
            default: sb.append(e);
          }
        } else {
          sb.append(c);
        }
      }
    }

    Object readNumber() {
      int start = pos;
      while (pos < s.length() && "+-0123456789.eE".indexOf(s.charAt(pos)) >= 0) pos++;
      if (start == pos) throw error("Beklenmeyen karakter '" + s.charAt(pos) + "'");
      try {
        return Double.parseDouble(s.substring(start, pos));
      } catch (NumberFormatException ex) {
        throw error("Gecersiz sayi");
      }
    }
  }
}
