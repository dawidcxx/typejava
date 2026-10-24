package cc.kilonode.typejava.runtime;

public final class Values {
  private Values() {}

  public static String toDisplayString(Object value) {
    if (value == null) {
      return "null";
    }
    if (value instanceof Double number) {
      if (Double.isNaN(number)) {
        return "NaN";
      }
      if (Double.isInfinite(number)) {
        return number > 0 ? "Infinity" : "-Infinity";
      }
      if (number == Math.rint(number) && number >= Long.MIN_VALUE && number <= Long.MAX_VALUE) {
        return Long.toString(number.longValue());
      }
      return number.toString();
    }
    return value.toString();
  }
}
