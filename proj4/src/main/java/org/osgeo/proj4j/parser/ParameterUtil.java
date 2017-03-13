/*
 * Copyright 2016 Martin Davis, Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
