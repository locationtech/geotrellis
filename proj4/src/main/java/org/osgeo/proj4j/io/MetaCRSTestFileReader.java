package org.osgeo.proj4j.io;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a file in MetaCRS Test format
 * into a list of {@link MetaCRSTestCase}.
 * This format is a CSV file with a standard set of columns.
 * Each record defines a transformation from one coordinate system
 * to another.
 * For full details on the file format, see http://trac.osgeo.org/metacrs/
 * 
 * @author Martin Davis
 *
 */
public class MetaCRSTestFileReader 
{
  public static final int COL_COUNT = 19;
  
  private File file;
  private CSVRecordParser lineParser = new CSVRecordParser();
  
  public MetaCRSTestFileReader(File file) 
  {
    this.file = file;
  }

  public List<MetaCRSTestCase> readTests()
  throws IOException
  {
    LineNumberReader lineReader = new LineNumberReader(new FileReader(file));
    List<MetaCRSTestCase> tests = null;
    try {
       tests = parseFile(lineReader);
    } 
    finally {
      lineReader.close();
    }
    return tests;
  }
  
  private List<MetaCRSTestCase> parseFile(LineNumberReader lineReader)
  throws IOException
  {
    List<MetaCRSTestCase> tests = new ArrayList<MetaCRSTestCase>();
    boolean isHeaderRead = false;
    while (true) {
      String line = lineReader.readLine();
      if (line == null)
        break;
      // skip comments
      if (line.startsWith("#"))
        continue;
      // skip header
      if (! isHeaderRead) {
        // TODO: validate header line to have correct set of columns
        isHeaderRead = true;
        continue;
      }
      tests.add(parseTest(line));
    }
    return tests;
  }
  
  private MetaCRSTestCase parseTest(String line)
  {
    String[] cols = lineParser.parse(line);
    if (cols.length != COL_COUNT)
      throw new IllegalStateException("Expected " + COL_COUNT+ " columns in file, but found " + cols.length);
    String testName    = cols[0];
    String testMethod  = cols[1];
    String srcCrsAuth  = cols[2];
    String srcCrs      = cols[3];
    String tgtCrsAuth  = cols[4];
    String tgtCrs      = cols[5];
    double srcOrd1     = parseNumber(cols[6]);
    double srcOrd2     = parseNumber(cols[7]);
    double srcOrd3     = parseNumber(cols[8]);
    double tgtOrd1     = parseNumber(cols[9]);
    double tgtOrd2     = parseNumber(cols[10]);
    double tgtOrd3     = parseNumber(cols[11]);
    double tolOrd1     = parseNumber(cols[12]);
    double tolOrd2     = parseNumber(cols[13]);
    double tolOrd3     = parseNumber(cols[14]);
    String using       = cols[15];
    String dataSource  = cols[16];
    String dataCmnts   = cols[17];
    String maintenanceCmnts = cols[18];
    
    return new MetaCRSTestCase(testName,testMethod,srcCrsAuth,srcCrs,tgtCrsAuth,tgtCrs,srcOrd1,srcOrd2,srcOrd3,tgtOrd1,tgtOrd2,tgtOrd3,tolOrd1,tolOrd2,tolOrd3,using,dataSource,dataCmnts,maintenanceCmnts);
  }
  
  /**
   * Parses a number from a String.
   * If the string is empty returns {@link Double.Nan}.
   * 
   * @param numStr
   * @return the number as a double
   * @return Double.NaN if the string is null or empty
   * @throws NumberFormatException if the number is ill-formed
   */
  private static double parseNumber(String numStr)
  {
    if (numStr == null || numStr.length() == 0) {
      return Double.NaN;
    }
    return Double.parseDouble(numStr);
  }
}
