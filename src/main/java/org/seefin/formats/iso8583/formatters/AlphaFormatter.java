package org.seefin.formats.iso8583.formatters;

import org.seefin.formats.iso8583.MessageException;
import org.seefin.formats.iso8583.types.CharEncoder;
import org.seefin.formats.iso8583.types.Dimension;
import org.seefin.formats.iso8583.types.FieldType;

import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;


/**
 * Formatter that can format and parse alpha field values
 * (includes alphanumeric, alpha+symbol, etc., e.g., non-numeric fields)
 * @author phillipsr
 */
public class AlphaFormatter extends TypeFormatter<String> {
  public AlphaFormatter(CharEncoder charset) {
    setCharset(charset);
  }

  /**
   * {@inheritDoc}
   * @param type variant of the alpha type specified for the field
   * @throws IllegalArgumentException if the data is null or invalid as an alpha string
   * @throws ParseException           if data cannot be translated to the appropriate charset
   */
  @Override
  public String parse(String type, Dimension dimension, int position, byte[] data)
      throws ParseException {
    String result;
    try {
      result = decode(data).trim();
    } catch (Exception e) {
      ParseException rethrow = new ParseException(
          "Decoding error for " + type + " field: " + Arrays.toString(data), position);
      rethrow.initCause(e);
      throw rethrow;
    }

    if (isValid(result, type, dimension) == false) {
      throw new ParseException("Invalid data parsed for field (" + type + ") value=[" + result + "]", position);
    }
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public byte[] format(String type, Object data, Dimension dimension) {
    if (data == null) {
      throw new IllegalArgumentException("Alpha values cannot be null");
    }
    String value = data instanceof byte[] ? new String((byte[]) data) : data.toString();

    if (isValid(value, type, dimension) == false) {
      throw new IllegalArgumentException("Cannot format invalid value for [" + type + "] field: '"
          + value + "', is-a " + value.getClass().getSimpleName());
    }

    if (dimension.getType() == Dimension.Type.FIXED) {
      int length = dimension.getLength();
      if (value.length() > length) {
        throw new MessageException("Fixed field data length ("
            + value.length() + ") exceeds field maximum (" + dimension.getLength() + "): data=[" + value + "]");
      }
      // for fixed width fields, pad right with spaces
      return String.format("%-" + length + "." + length + "s", value).getBytes();
    }
    // Variable field: dim length is the maximum length:
    if (value.length() > dimension.getLength()) {
      throw new MessageException("Variable field data length ("
          + value.length() + ") exceeds field maximum (" + dimension.getLength() + ")");
    }
    return value.getBytes();
  }

  /* set of pattern matchers for the various alpha-based type fields */
  private static final Map<String, Pattern> Validators = new HashMap<String, Pattern>(3) {{
    put(FieldType.ALPHA, Pattern.compile("[a-zA-Z]*"));      // zero or more alphabetic
    put(FieldType.ALPHANUM, Pattern.compile("[a-zA-Z0-9]*"));   // zero or more alphabetic or digit
    put(FieldType.ALPHANUMPAD, Pattern.compile("[a-zA-Z 0-9]*"));  // zero or more alphabetic, digit or space
    put(FieldType.ALPHASYMBOL, Pattern.compile("[ -~&&[^0-9]]*")); // zero or more alphabetic or symbol
    put(FieldType.ALPHANUMSYMBOL, Pattern.compile("[ -~]*"));      // zero or more any character
    put(FieldType.NUMSYMBOL, Pattern.compile("[ -~&&[^a-zA-Z]]*")); // zero or more symbol
    put(FieldType.TRACKDATA, Pattern.compile("[ -~]*"));        // zero or more any character
  }};

  /**
   * {@inheritDoc}
   * <p/>checks the string representation of <code>value</code> against the
   * pattern matcher for the supplied <code>type</code>
   */
  @Override
  public boolean isValid(Object value, String type, Dimension dim) {
    return value != null && Validators.get(type).matcher(value.toString().trim()).matches();
  }

}
