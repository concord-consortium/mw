/* $RCSfile: Parser.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:17 $
 * $Revision: 1.1 $
 *
 * Copyright (C) 2006  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.myjmol.util;

public class Parser {

  /// for adapter (and others?) ///
  
  public int ichNextParse;

  public float parseFloat(String str) {
    return parseFloatChecked(str, 0, str.length());
  }

  public float parseFloat(String str, int ich) {
    int cch = str.length();
    if (ich >= cch)
      return Float.NaN;
    return parseFloatChecked(str, ich, cch);
  }

  public float parseFloat(String str, int ichStart, int ichMax) {
    int cch = str.length();
    if (ichMax > cch)
      ichMax = cch;
    if (ichStart >= ichMax)
      return Float.NaN;
    return parseFloatChecked(str, ichStart, ichMax);
  }

  private final static float[] decimalScale = { 0.1f, 0.01f, 0.001f, 0.0001f, 0.00001f,
      0.000001f, 0.0000001f, 0.00000001f };
  private final static float[] tensScale = { 10, 100, 1000, 10000, 100000, 1000000 };

  private float parseFloatChecked(String str, int ichStart, int ichMax) {
    boolean digitSeen = false;
    float value = 0;
    int ich = ichStart;
    char ch;
    while (ich < ichMax && ((ch = str.charAt(ich)) == ' ' || ch == '\t'))
      ++ich;
    boolean negative = false;
    if (ich < ichMax && str.charAt(ich) == '-') {
      ++ich;
      negative = true;
    }
    ch = 0;
    while (ich < ichMax && (ch = str.charAt(ich)) >= '0' && ch <= '9') {
      value = value * 10 + (ch - '0');
      ++ich;
      digitSeen = true;
    }
    if (ch == '.') {
      int iscale = 0;
      while (++ich < ichMax && (ch = str.charAt(ich)) >= '0' && ch <= '9') {
        if (iscale < decimalScale.length)
          value += (ch - '0') * decimalScale[iscale];
        ++iscale;
        digitSeen = true;
      }
    }
    if (!digitSeen)
      value = Float.NaN;
    else if (negative)
      value = -value;
    if (ich < ichMax && (ch == 'E' || ch == 'e' || ch == 'D')) {
      if (++ich >= ichMax)
        return Float.NaN;
      ch = str.charAt(ich);
      if ((ch == '+') && (++ich >= ichMax))
        return Float.NaN;
      int exponent = parseIntChecked(str, ich, ichMax);
      if (exponent == Integer.MIN_VALUE)
        return Float.NaN;
      if (exponent > 0)
        value *= ((exponent < tensScale.length) ? tensScale[exponent - 1]
            : Math.pow(10, exponent));
      else if (exponent < 0)
        value *= ((-exponent < decimalScale.length) ? decimalScale[-exponent - 1]
            : Math.pow(10, exponent));
    } else {
      ichNextParse = ich; // the exponent code finds its own ichNextParse
    }
    //Logger.debug("parseFloat(" + str + "," + ichStart + "," +
    // ichMax + ") -> " + value);
    return value;
  }

  /**
   * parses a string for an integer
   * @param str
   * @return integer or Integer.MIN_VALUE
   */
  public int parseInt(String str) {
    return parseIntChecked(str, 0, str.length());
  }

  public int parseInt(String str, int ich) {
    int cch = str.length();
    if (ich >= cch)
      return Integer.MIN_VALUE;
    return parseIntChecked(str, ich, cch);
  }

  public int parseInt(String str, int ichStart, int ichMax) {
    int cch = str.length();
    if (ichMax > cch)
      ichMax = cch;
    if (ichStart >= ichMax)
      return Integer.MIN_VALUE;
    return parseIntChecked(str, ichStart, ichMax);
  }

  private int parseIntChecked(String str, int ichStart, int ichMax) {
    boolean digitSeen = false;
    int value = 0;
    int ich = ichStart;
    char ch;
    while (ich < ichMax && ((ch = str.charAt(ich)) == ' ' || ch == '\t'))
      ++ich;
    boolean negative = false;
    if (ich < ichMax && str.charAt(ich) == '-') {
      negative = true;
      ++ich;
    }
    while (ich < ichMax && (ch = str.charAt(ich)) >= '0' && ch <= '9') {
      value = value * 10 + (ch - '0');
      digitSeen = true;
      ++ich;
    }
    if (!digitSeen)
      value = Integer.MIN_VALUE;
    else if (negative)
      value = -value;
    //Logger.debug("parseInt(" + str + "," + ichStart + "," +
    // ichMax + ") -> " + value);
    ichNextParse = ich;
    return value;
  }

  public String[] getTokens(String line) {
    return getTokens(line, 0);
  }

  public String[] getTokens(String line, int ich) {
    if (line == null)
      return null;
    int cchLine = line.length();
    if (ich > cchLine)
      return null;
    int tokenCount = countTokens(line, ich);
    String[] tokens = new String[tokenCount];
    ichNextParse = ich;
    for (int i = 0; i < tokenCount; ++i)
            tokens[i] = parseTokenChecked(line, ichNextParse, cchLine);
    /*
     Logger.debug("-----------\nline:" + line);
     for (int i = 0; i < tokenCount; ++i) 
     Logger.debug("token[" + i + "]=" + tokens[i]);
     */
    return tokens;
  }

  public int countTokens(String line, int ich) {
    int tokenCount = 0;
    if (line != null) {
      int ichMax = line.length();
      char ch;
      while (true) {
        while (ich < ichMax && ((ch = line.charAt(ich)) == ' ' || ch == '\t'))
          ++ich;
        if (ich == ichMax)
          break;
        ++tokenCount;
        do {
          ++ich;
        } while (ich < ichMax && ((ch = line.charAt(ich)) != ' ' && ch != '\t'));
      }
    }
    return tokenCount;
  }

  public String parseToken(String str) {
    return parseTokenChecked(str, 0, str.length());
  }

  public String parseToken(String str, int ich) {
    int cch = str.length();
    if (ich >= cch)
      return null;
    return parseTokenChecked(str, ich, cch);
  }

  public String parseToken(String str, int ichStart, int ichMax) {
    int cch = str.length();
    if (ichMax > cch)
      ichMax = cch;
    if (ichStart >= ichMax)
      return null;
    return parseTokenChecked(str, ichStart, ichMax);
  }

  private String parseTokenChecked(String str, int ichStart, int ichMax) {
    int ich = ichStart;
    char ch;
    while (ich < ichMax && ((ch = str.charAt(ich)) == ' ' || ch == '\t'))
      ++ich;
    int ichNonWhite = ich;
    while (ich < ichMax && ((ch = str.charAt(ich)) != ' ' && ch != '\t'))
      ++ich;
    ichNextParse = ich;
    if (ichNonWhite == ich)
      return null;
    return str.substring(ichNonWhite, ich);
  }

  public String parseTrimmed(String str) {
    return parseTrimmedChecked(str, 0, str.length());
  }

  public String parseTrimmed(String str, int ich) {
    int cch = str.length();
    if (ich >= cch)
      return "";
    return parseTrimmedChecked(str, ich, cch);
  }

  public String parseTrimmed(String str, int ichStart, int ichMax) {
    int cch = str.length();
    if (ichMax > cch)
      ichMax = cch;
    if (ichStart >= ichMax)
      return "";
    return parseTrimmedChecked(str, ichStart, ichMax);
  }

  private String parseTrimmedChecked(String str, int ichStart, int ichMax) {
    int ich = ichStart;
    char ch;
    while (ich < ichMax && ((ch = str.charAt(ich)) == ' ' || ch == '\t'))
      ++ich;
    int ichLast = ichMax - 1;
    while (ichLast >= ich && ((ch = str.charAt(ichLast)) == ' ' || ch == '\t'))
      --ichLast;
    if (ichLast < ich)
      return "";
    ichNextParse = ichLast + 1;
    return str.substring(ich, ichLast + 1);
  }

  public String concatTokens(String[] tokens, int iFirst, int iEnd) {
    String str = "";
    String sep = "";
    for (int i = iFirst; i < iEnd; i++) {
      if (i < tokens.length) {
        str += sep + tokens[i];
        sep = " ";
      }
    }
    return str;
  }
  
  public String getString(String line, String strQuote) {
    int i = line.indexOf(strQuote);
    int j = line.lastIndexOf(strQuote);
    return (j == i ? "" : line.substring(i + 1, j));
  }
  
}
