package org.osgeo.proj4j.cmd;


import java.io.*;
import java.util.*;

import org.osgeo.proj4j.*;
import org.osgeo.proj4j.io.*;
import org.osgeo.proj4j.util.*;

/**
 * A command-line application which runs test files 
 * in MetaCRS Transformation Test format.
 * <p>
 * Usage:
 * <pre>
 *   MetaCRSTestCmd <test-file-name>
 * </pre>
 * 
 * @author Martin Davis
 *
 */
public class MetaCRSTestCmd 
{
  // TODO: display line numbers of tests in verbose mode
  public static void main(String args[]) 
  {
    MetaCRSTestCmd cmd = new MetaCRSTestCmd();
    cmd.parseArgs(args);
    try {
      cmd.execute();
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  private static String usage()
  {
    return "Usage: MetaCRSTestCmd [-verbose] { <test-file-name> }";
  }
  private static final int TESTS_PER_LINE = 50;
  
  private static CRSFactory csFactory = new CRSFactory();

  private List<String> filenames = new ArrayList<String>();
  private boolean verbose = false;
  
  int count = 0;
  int failCount = 0;
  int errCount = 0;

  private CRSCache crsCache = new CRSCache();
  
  public MetaCRSTestCmd() 
  {
  }
  
  private void parseArgs(String[] args)
  {
    //TODO: error handling
    if (args.length <= 0) {
      System.err.println(usage());
      System.exit(1);
    }
    parseFlags(args);
    
    parseFiles(args);
  }

  private void parseFlags(String[] args)
  {
    for (String arg : args) {
      if (arg.startsWith("-")) {
        if (arg.equalsIgnoreCase("-verbose")) {
          verbose = true;
        }
      }
    }
  }
  private void parseFiles(String[] args)
  {
    for (String arg : args) {
      if (! arg.startsWith("-")) {
        filenames.add(arg);
      }
    }
  }
  private void execute()
  throws IOException
  {
    long timeInMillis = System.currentTimeMillis();
    for (String filename : filenames) {
      execute(filename);
    }
    
    System.out.println();
    System.out.println("Tests run: " + count
        + ",  Failures: " + failCount
        + ",  Errors: " + errCount);

    long timeMS = System.currentTimeMillis() - timeInMillis;
    System.out.println("Time: " + (timeMS / 1000.0) + " s");
  }
  
  private void execute(String filename)
  throws IOException
  {
    System.out.println("File: " + filename);
    
    File file = new File(filename);
    MetaCRSTestFileReader reader = new MetaCRSTestFileReader(file);
    List<MetaCRSTestCase> tests = reader.readTests();
    
    for (MetaCRSTestCase test : tests) 
    {
      test.setCache(crsCache);
      count++;
      System.out.print(".");
      boolean isOk;
      try {
        isOk = test.execute(csFactory);
      }
      catch (Proj4jException ex) {
        System.out.println(ex);
        errCount++;
        continue;
      }
      if (! isOk) {
        failCount++;
        System.out.print("F");
      }
      if (verbose || ! isOk) {
        System.out.println();
        test.print(System.out);
      }

      if (count % TESTS_PER_LINE == 0)
        System.out.println();
    }
    System.out.println();   
  }

}
