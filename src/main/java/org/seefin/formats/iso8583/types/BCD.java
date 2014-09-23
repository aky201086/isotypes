package org.seefin.formats.iso8583.types;

import org.apache.commons.lang.ArrayUtils;

import java.math.BigInteger;
import java.util.regex.Pattern;

/**
 * Utilities to manipulate (Packed) Binary Coded Decimal values, as no standard
 * third-party library found (mail me if you know of one...)
 * @author Converted from an old C library (author unknown)
 */
public class BCD {
  /** regex to validate values passed in as strings */
  private static final Pattern NumberMatcher = Pattern.compile("[0-9]*");

  /**
   * Answer with a byte array being the BCD representation of the numberic string supplied
   * @param value string representation of the number to convert
   * @return BCD byte array
   * @throws IllegalArgumentException if the supplied value is not a valid numeric string
   */
  public static byte[] valueOf(String value) {
    if (value == null) {
      throw new IllegalArgumentException("Cannot convert <null> to BCD");
    }
    value = value.trim();
    if (!NumberMatcher.matcher(value).matches()) {
      throw new IllegalArgumentException("Can only convert strings of digits to BCD");
    }
    byte[] result = valueOf(new BigInteger(value));
    int vlength = value.length() / 2;
    while (result.length < vlength) {
      result = ArrayUtils.add(result, 0, (byte) 0);
    }
    return result;
  }

  /**
   * Answer with a byte array being the BCD representation of the long supplied
   * @param value
   * @return
   */
  public static byte[] valueOf(long value) {
    int nbDigits = (int) (Math.log10(value) + 1);
    int bytes = nbDigits % 2 == 0 ? nbDigits / 2 : (nbDigits + 1) / 2;
    byte result[] = new byte[bytes];

    for (int i = 0; i < nbDigits; i++) {
      byte register = (byte) (value % 10);
      value /= 10;
      if ((i % 2 == 0) || (i == nbDigits - 1 && (nbDigits % 2 != 0))) {
        result[i / 2] = register;
        continue;
      }
      register = (byte) (register << 4);
      result[i / 2] |= register;
    }

    swapOrder(bytes, result);
    return result;
  }

  /**
   * Answer with a byte array being the BCD representation of the BigInteger supplied
   * @param value
   * @return
   * @throws IllegalArgumentException if the value is null
   */
  public static byte[] valueOf(BigInteger value) {
    if (value == null) {
      throw new IllegalArgumentException("Cannot convert <null> to BCD");
    }
    int nbDigits = (int) (Math.log10(value.doubleValue()) + 1);
    int bytes = nbDigits % 2 == 0 ? nbDigits / 2 : (nbDigits + 1) / 2;
    byte result[] = new byte[bytes];

    for (int i = 0; i < nbDigits; i++) {
      byte register = value.mod(BigInteger.TEN).byteValue();
      value = value.divide(BigInteger.TEN);
      if ((i % 2 == 0) || (i == nbDigits - 1 && (nbDigits % 2 != 0))) {
        result[i / 2] = register;
        continue;
      }
      register = (byte) (register << 4);
      result[i / 2] |= register;
    }

    swapOrder(bytes, result);
    return result;
  }

  private static void swapOrder(int bytes, byte[] result) {
    for (int i = 0; i < bytes / 2; i++) {
      byte register = result[i];
      result[i] = result[bytes - i - 1];
      result[bytes - i - 1] = register;
    }
  }

  /**
   * Answer with a string representation of the BCD
   * (byte) value supplied
   * @param value
   * @return
   */
  public static String toString(byte value) {
    byte[] nibbles = new byte[2];
    nibbles[1] = (byte) ((value & 0x0f));
    byte b = (byte) (value & 0xf0);
    b >>>= 4;
    nibbles[0] = (byte) ((b & 0x0f));
    return "" + nibbles[0] + nibbles[1];
  }

  public static String toString(byte[] bcd) {
    StringBuilder result = new StringBuilder();
    for (byte b : bcd) {
      result.append(toString(b));
    }

    return result.toString();
  }

}