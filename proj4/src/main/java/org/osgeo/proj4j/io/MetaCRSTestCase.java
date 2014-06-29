package org.osgeo.proj4j.io;

import java.io.*;
import org.osgeo.proj4j.*;
import org.osgeo.proj4j.util.*;

public class MetaCRSTestCase 
{
	private static final CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
	
  private boolean verbose = true;
  
  String testName;
  String testMethod;
  
  String srcCrsAuth;
  String srcCrs;
  
  String tgtCrsAuth;
  String tgtCrs;
  
  double srcOrd1;
  double srcOrd2;
  double srcOrd3;
  
  double tgtOrd1;
  double tgtOrd2;
  double tgtOrd3;
  
  double tolOrd1;
  double tolOrd2;
  double tolOrd3;
  
  String using;
  String dataSource;
  String dataCmnts;
  String maintenanceCmnts;

  CoordinateReferenceSystem srcCS;
  CoordinateReferenceSystem tgtCS;

  ProjCoordinate srcPt = new ProjCoordinate();
  ProjCoordinate resultPt = new ProjCoordinate();

  private boolean isInTol;
  private CRSCache crsCache = null;
  
  public MetaCRSTestCase(
      String testName,
      String testMethod,
      String srcCrsAuth,
      String srcCrs,
      String tgtCrsAuth,
      String tgtCrs,
      double srcOrd1,
      double srcOrd2,
      double srcOrd3,
      double tgtOrd1,
      double tgtOrd2,
      double tgtOrd3,
      double tolOrd1,
      double tolOrd2,
      double tolOrd3,
      String using,
      String dataSource,
      String dataCmnts,
      String maintenanceCmnts
  )
  {
    this.testName = testName;
    this.testMethod = testMethod;
    this.srcCrsAuth = srcCrsAuth;
    this.srcCrs = srcCrs;
    this.tgtCrsAuth = tgtCrsAuth;
    this.tgtCrs = tgtCrs;
    this.srcOrd1 = srcOrd1;
    this.srcOrd2 = srcOrd2;
    this.srcOrd3 = srcOrd3;
    this.tgtOrd1 = tgtOrd1;
    this.tgtOrd2 = tgtOrd2;
    this.tgtOrd3 = tgtOrd3;
    this.tolOrd1 = tolOrd1;
    this.tolOrd2 = tolOrd2;
    this.tolOrd3 = tolOrd3;
    this.using = using;
    this.dataSource = dataSource;
    this.dataCmnts = dataCmnts;
    this.maintenanceCmnts = maintenanceCmnts;
  }

  public String getName() { return testName; }
  
  public String getSourceCrsName() { return csName(srcCrsAuth, srcCrs); }
  
  public String getTargetCrsName() { return csName(tgtCrsAuth, tgtCrs); }
  
  public CoordinateReferenceSystem getSourceCS() { return srcCS; }
  
  public CoordinateReferenceSystem getTargetCS() { return tgtCS; }
  
  public ProjCoordinate getSourceCoordinate()
  {
    return new ProjCoordinate(srcOrd1, srcOrd2, srcOrd3);
  }
  
  public ProjCoordinate getTargetCoordinate()
  {
    return new ProjCoordinate(tgtOrd1, tgtOrd2, tgtOrd3);
  }
  
  public ProjCoordinate getResultCoordinate()
  {
    return new ProjCoordinate(resultPt.x, resultPt.y);
  }
  
  public void setCache(CRSCache crsCache)
  {
    this.crsCache = crsCache;
  }
  
  public boolean execute(CRSFactory csFactory)
  {
    boolean isOK = false;
    srcCS = createCS(csFactory, srcCrsAuth, srcCrs);
    tgtCS = createCS(csFactory, tgtCrsAuth, tgtCrs);
    isOK = executeTransform(srcCS, tgtCS);
    return isOK;
  }
  
  public static String csName(String auth, String code)
  {
    return auth + ":" + code;
  }
  
  public CoordinateReferenceSystem createCS(CRSFactory csFactory, String auth, String code)
  {
    String name = csName(auth, code);
    
    if (crsCache != null) {
      return crsCache.createFromName(name);
    }
    CoordinateReferenceSystem cs = csFactory.createFromName(name);
    return cs;
  }
  
  private boolean executeTransform(
      CoordinateReferenceSystem srcCS,
      CoordinateReferenceSystem tgtCS)
  {
    srcPt.x = srcOrd1;
    srcPt.y = srcOrd2;
    // Testing: flip axis order to test SS sample file
    //srcPt.x = srcOrd2;
    //srcPt.y = srcOrd1;
    
    CoordinateTransform trans = ctFactory.createTransform(srcCS, tgtCS);

    trans.transform(srcPt, resultPt);
    
    double dx = Math.abs(resultPt.x - tgtOrd1);
    double dy = Math.abs(resultPt.y - tgtOrd2);
    
    isInTol =  dx <= tolOrd1 && dy <= tolOrd2;

    return isInTol;
  }

  public void print(PrintStream os)
  {
      System.out.println(testName);
      System.out.println(ProjectionUtil.toString(srcPt) 
          + " -> " + ProjectionUtil.toString(resultPt)
          + " ( expected: " + tgtOrd1 + ", " + tgtOrd2 + " )"
          );

    
    if (! isInTol) {
      System.out.println("FAIL");
      System.out.println("Src CRS: (" 
          + srcCrsAuth + ":" + srcCrs + ") " 
          + srcCS.getParameterString());
      System.out.println("Tgt CRS: ("
          + tgtCrsAuth + ":" + tgtCrs + ") " 
          + tgtCS.getParameterString());
    }
  }
}
