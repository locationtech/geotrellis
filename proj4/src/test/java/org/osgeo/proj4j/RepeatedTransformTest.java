package org.osgeo.proj4j;

import junit.framework.Assert;
import junit.framework.TestCase;
import junit.textui.TestRunner;

public class RepeatedTransformTest extends TestCase
{
  public static void main(String args[]) {
    TestRunner.run(RepeatedTransformTest.class);
  }

  public void testRepeatedTransform()
  {
    CRSFactory crsFactory = new CRSFactory();

    CoordinateReferenceSystem src = crsFactory.createFromName("epsg:4326");
    CoordinateReferenceSystem dest = crsFactory.createFromName("epsg:27700");

    CoordinateTransformFactory ctf = new CoordinateTransformFactory();
    CoordinateTransform transform = ctf.createTransform(src, dest);
    
    ProjCoordinate srcPt = new ProjCoordinate(0.899167, 51.357216);
    ProjCoordinate destPt = new ProjCoordinate();
   
    transform.transform(srcPt, destPt);
    System.out.println(srcPt + " ==> " + destPt);
    
    // do it again
    ProjCoordinate destPt2 = new ProjCoordinate();
    transform.transform(srcPt, destPt2);
    System.out.println(srcPt + " ==> " + destPt2);

    assertTrue(destPt.equals(destPt2));
  }
}
