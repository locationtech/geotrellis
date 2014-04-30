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
