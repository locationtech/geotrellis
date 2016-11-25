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

package org.osgeo.proj4j.util;

import java.util.HashMap;
import java.util.Map;
import org.osgeo.proj4j.*;

public class CRSCache 
{
  private static Map<String, CoordinateReferenceSystem> projCache = new HashMap<String, CoordinateReferenceSystem>();
  private static CRSFactory crsFactory = new CRSFactory();

// TODO: provide limit on number of items in cache (LRU)
  
  public CRSCache() {
    super();
  }

  public CoordinateReferenceSystem createFromName(String name)
  throws UnsupportedParameterException, InvalidValueException, UnknownAuthorityCodeException
  {
    CoordinateReferenceSystem proj = (CoordinateReferenceSystem) projCache.get(name);
    if (proj == null) {
      proj = crsFactory.createFromName(name);
      projCache.put(name, proj);
    }
    return proj;
  }

}
