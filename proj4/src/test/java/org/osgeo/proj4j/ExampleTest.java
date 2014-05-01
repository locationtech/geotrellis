package org.osgeo.proj4j;

import junit.framework.TestCase;
import junit.textui.TestRunner;

/**
 * Test which serves as an example of using Proj4J.
 * 
 * @author mbdavis
 *
 */
public class ExampleTest extends TestCase
{
  public static void main(String args[]) {
    TestRunner.run(ExampleTest.class);
  }

  public ExampleTest(String name) { super(name); }

  public void testTransformToGeographic()
  {
    assertTrue(checkTransform("EPSG:2227", -121.3128278, 37.95657778, 6327319.23 , 2171792.15, 0.01 ));
  }
  
  private boolean checkTransform(String csName, double lon, double lat, double expectedX, double expectedY, double tolerance)
  {
  	CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
  	CRSFactory csFactory = new CRSFactory();
  	/*
  	 * Create {@link CoordinateReferenceSystem} & CoordinateTransformation.
  	 * Normally this would be carried out once and reused for all transformations
  	 */ 
    CoordinateReferenceSystem crs = csFactory.createFromName(csName);
    
    final String WGS84_PARAM = "+title=long/lat:WGS84 +proj=longlat +ellps=WGS84 +datum=WGS84 +units=degrees";
    CoordinateReferenceSystem WGS84 = csFactory.createFromParameters("WGS84",WGS84_PARAM);

    CoordinateTransform trans = ctFactory.createTransform(WGS84, crs);
    
    /*
     * Create input and output points.
     * These can be constructed once per thread and reused.
     */ 
    ProjCoordinate p = new ProjCoordinate();
    ProjCoordinate p2 = new ProjCoordinate();
    p.x = lon;
    p.y = lat;
    
    /*
     * Transform point
     */
    trans.transform(p, p2);
    
   
    return isInTolerance(p2, expectedX, expectedY, tolerance);
  }

  public void testExplicitTransform()
  {
    String csName1 = "EPSG:32636";
    String csName2 = "EPSG:4326";
    
    CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
    CRSFactory csFactory = new CRSFactory();
    /*
     * Create {@link CoordinateReferenceSystem} & CoordinateTransformation.
     * Normally this would be carried out once and reused for all transformations
     */ 
    CoordinateReferenceSystem crs1 = csFactory.createFromName(csName1);
    CoordinateReferenceSystem crs2 = csFactory.createFromName(csName2);

    CoordinateTransform trans = ctFactory.createTransform(crs1, crs2);
    
    /*
     * Create input and output points.
     * These can be constructed once per thread and reused.
     */ 
    ProjCoordinate p1 = new ProjCoordinate();
    ProjCoordinate p2 = new ProjCoordinate();
    p1.x = 500000;
    p1.y = 4649776.22482;
    
    /*
     * Transform point
     */
    trans.transform(p1, p2);
    
    assertTrue(isInTolerance(p2, 33, 42, 0.000001));
  }
  

  boolean isInTolerance(ProjCoordinate p, double x, double y, double tolerance)
  {
    /*
     * Compare result to expected, for test purposes
     */ 
    double dx = Math.abs(p.x - x);
    double dy = Math.abs(p.y - y);
    boolean isInTol =  dx <= tolerance && dy <= tolerance;
    return isInTol;
  }

}
