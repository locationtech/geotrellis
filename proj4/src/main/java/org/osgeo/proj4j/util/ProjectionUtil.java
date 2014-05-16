package org.osgeo.proj4j.util;

import org.osgeo.proj4j.ProjCoordinate;

public class ProjectionUtil 
{
  public static String toString(ProjCoordinate p)
  {
    return "[" + p.x + ", " + p.y + "]";
  }

}
