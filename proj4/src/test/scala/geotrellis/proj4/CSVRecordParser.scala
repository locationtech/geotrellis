package geotrellis.proj4

/**
 * Parses a single record in a CSV file into an array of {@link String}s.
 * 
 * 
 * @author Martin Davis (port by Rob Emanuele)
 *
 */
class CSVRecordParser() {
  private final val CH_QUOTE = 1
  private final val CH_WHITESPACE = 2
  private final val CH_DATA = 3
  private final val CH_SEPARATOR = 4
  private final val CH_EOL = 5

  private final val STATE_DATA = 1
  private final val STATE_BEFORE = 2
  private final val STATE_QUOTED_DATA = 3
  private final val STATE_SEEN_QUOTE = 4
  private final val STATE_AFTER = 5
  
  private val quote = '"'
  private val separator = ','
  
  private var loc = 0

  /**
   * Parses a single record of a CSV file.
   * 
   * @param record
   * @return an array of the fields in the record
   * @throws IllegalArgumentException if the parsing of a field fails
   */
  def parse(record: String): List[String] = {
    loc = 0
    val vals = scala.collection.mutable.ListBuffer[String]()
    val lineLen = record.length()
    while (loc < lineLen) {
      vals += parseField(record)
    }
    vals.toList
  }
  
  private def parseField(line: String): String = {
    var category = CH_EOL

    if (loc < line.length())
      category = categorize(line.charAt(loc))

    if(category == CH_EOL) {
      null
    } else {
      val data = new StringBuffer

      var state = STATE_BEFORE

      var stop = false
      while(!stop) {        
        state match {
          case STATE_BEFORE =>
            category match {
              case CH_WHITESPACE => // pass
              case CH_QUOTE =>
                state = STATE_QUOTED_DATA
              case CH_SEPARATOR =>
                stop = true
              case CH_DATA =>
                data.append(line.charAt(loc))
                state = STATE_DATA
              case _ => sys.error("Invalid category")
            }

          case STATE_DATA =>
            category match {
              case CH_SEPARATOR | CH_EOL =>
                stop = true
              case CH_QUOTE =>
                data.append(line.charAt(loc))
              case CH_WHITESPACE | CH_DATA =>
                data.append(line.charAt(loc))
            }

          case STATE_QUOTED_DATA =>
            category match {
              case CH_QUOTE =>
                state = STATE_SEEN_QUOTE
              case CH_SEPARATOR | CH_WHITESPACE | CH_DATA =>
                data.append(line.charAt(loc))
              case CH_EOL =>
                stop = true
                loc -= 1
            }
          case STATE_SEEN_QUOTE =>
            category match {
              case CH_QUOTE =>
                // double quote - add to value
                data.append('"')
                state = STATE_QUOTED_DATA
              case CH_SEPARATOR |CH_EOL =>
                // at end of field
                stop = true
              case CH_WHITESPACE =>
                state = STATE_AFTER
              case CH_DATA =>
                throw new IllegalArgumentException("Malformed field - quote not at end of field")
            }
          case STATE_AFTER =>
            category match {
              case CH_QUOTE =>
                throw new IllegalArgumentException("Malformed field - unexpected quote")
              case CH_EOL |CH_SEPARATOR =>
                // at end of field
                stop = true
              case CH_WHITESPACE =>
                // skip trailing whitespace
              case CH_DATA =>
                throw new IllegalArgumentException("Malformed field - unexpected data after quote")
            }
        }
        loc += 1
      }
      data.toString()
    }
  }
  
  /**
   * Categorizes a character into a lexical category.
   * 
   * @param c the character to categorize
   * @return the lexical category
   */
  private def categorize(c: Char): Int = 
    if(c == ' ' || c == '\r' || c == 0xff || c == '\n') {
      CH_WHITESPACE
    } else if (c == quote) {
      CH_QUOTE
    } else if (c == separator) {
      CH_SEPARATOR
    } else if ('!' <= c && c <= '~') {
      CH_DATA
    } else if (0x00 <= c && c <= 0x20) {
      CH_WHITESPACE
    } else if (Character.isWhitespace(c)) {
      CH_WHITESPACE
    } else {
      CH_DATA
    }
}
