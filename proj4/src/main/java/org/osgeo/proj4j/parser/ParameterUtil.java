package org.osgeo.proj4j.parser;

import org.osgeo.proj4j.units.Angle;
import org.osgeo.proj4j.units.AngleFormat;

public class ParameterUtil {

  public static final AngleFormat format = new AngleFormat( AngleFormat.ddmmssPattern, true );

  /**
   * 
   * @param s
   * @return
   * @deprecated
   * @see Angle#parse(String)
   */
  public static double parseAngle( String s ) {
    return format.parse( s, null ).doubleValue();
  }
}
