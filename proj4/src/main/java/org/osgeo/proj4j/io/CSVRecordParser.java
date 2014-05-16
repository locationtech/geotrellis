package org.osgeo.proj4j.io;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses a single record in a CSV file into an array of {@link String}s.
 * 
 * 
 * @author Martin Davis
 *
 */
public class CSVRecordParser 
{
  private static final int CH_QUOTE = 1;
  private static final int CH_WHITESPACE = 2;
  private static final int CH_DATA = 3;
  private static final int CH_SEPARATOR = 4;
  private static final int CH_EOL = 5;

  private static final int STATE_DATA = 1;
  private static final int STATE_BEFORE = 2;
  private static final int STATE_QUOTED_DATA = 3;
  private static final int STATE_SEEN_QUOTE = 4;
  private static final int STATE_AFTER = 5;
  
  private static final String[] strArrayType = new String[0];
  
  private char quote = '"';
  private char separator = ',';
  
  private int loc = 0;
  
  /**
   * Controls whether the parsing strictly follows the CSV specification.
   * If not in strict mode:
   * <ul>
   * <li>quotes which occur in the middle of fields are simply scanned as data
   * </ul>
   */
  private boolean isStrictMode = false;
  
  /**
   * Creates a new parser.
   *
   */
  public CSVRecordParser() {
  }

  /**
   * Parses a single record of a CSV file.
   * 
   * @param record
   * @return an array of the fields in the record
   * @throws IllegalArgumentException if the parsing of a field fails
   */
  public String[] parse(String record)
  {
    loc = 0;
    List vals = new ArrayList();
    int lineLen = record.length();
    while (loc < lineLen) {
      vals.add(parseField(record));
    }
    return (String[]) vals.toArray(strArrayType);
  }
  
  private String parseField(String line)
  {
    StringBuffer data = new StringBuffer();
    
    int state = STATE_BEFORE;
    while (true) {
      int category = CH_EOL;
      if (loc < line.length())
        category = categorize(line.charAt(loc));
      
      switch (state) {
      case STATE_BEFORE:
        switch (category) {
        case CH_WHITESPACE:
          loc++;  
          break;
        case CH_QUOTE:
          loc++;
          state = STATE_QUOTED_DATA;
          break;
        case CH_SEPARATOR:
          loc++;
          return "";
        case CH_DATA:
          data.append(line.charAt(loc));
          state = STATE_DATA;
          loc++;
          break;
        case CH_EOL:
          return null;
        }
        break;
      case STATE_DATA:
        switch (category) {
        case CH_SEPARATOR:
        case CH_EOL:
          loc++;
          return data.toString();
        case CH_QUOTE:
          if (isStrictMode) {
            throw new IllegalArgumentException("Malformed field - quote not at beginning of field");
          }
          else {
            data.append(line.charAt(loc));
            loc++;
          }
          break;
        case CH_WHITESPACE:
        case CH_DATA:
          data.append(line.charAt(loc));
          loc++;
          break;
        }
        break;
      case STATE_QUOTED_DATA:
        switch (category) {
        case CH_QUOTE:
          loc++;
          state = STATE_SEEN_QUOTE;
          break;
        case CH_SEPARATOR:
        case CH_WHITESPACE:
        case CH_DATA:
          data.append(line.charAt(loc));
          loc++;
          break;
        case CH_EOL:
          return data.toString();
        }
        break;
      case STATE_SEEN_QUOTE:
        switch (category) {
        case CH_QUOTE:
          // double quote - add to value
          loc++;
          data.append('"');
          state = STATE_QUOTED_DATA;
          break;
        case CH_SEPARATOR:
        case CH_EOL:
          // at end of field
          loc++;
          return data.toString();
        case CH_WHITESPACE:
          loc++;
          state = STATE_AFTER;
          break;
        case CH_DATA:
          throw new IllegalArgumentException("Malformed field - quote not at end of field");
        }
        break;
      case STATE_AFTER:
        switch (category) {
        case CH_QUOTE:
          throw new IllegalArgumentException("Malformed field - unexpected quote");
        case CH_EOL:
        case CH_SEPARATOR:
          // at end of field
          loc++;
          return data.toString();
        case CH_WHITESPACE:
          // skip trailing whitespace
          loc++;
          break;
        case CH_DATA:
          throw new IllegalArgumentException("Malformed field - unexpected data after quote");
        }
      
      }
    }
  }
  
  /**
   * Categorizes a character into a lexical category.
   * 
   * @param c the character to categorize
   * @return the lexical category
   */
  private int categorize(char c) {
    switch (c) {
    case ' ':
    case '\r':
    case 0xff:
    case '\n':
      return CH_WHITESPACE;
    default:
      if (c == quote) {
        return CH_QUOTE;
      } else if (c == separator) {
        return CH_SEPARATOR;
      }
      else if ('!' <= c && c <= '~') {
        return CH_DATA;
      } else if (0x00 <= c && c <= 0x20) {
        return CH_WHITESPACE;
      } else if (Character.isWhitespace(c)) {
        return CH_WHITESPACE;
      } else {
        return CH_DATA;
      }
    }
  }
}
