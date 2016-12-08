# Questions During the Parsing of the EPSG.properties file

[WKT format](http://www.geoapi.org/3.0/javadoc/org/opengis/referencing/doc-files/WKT.html)

1. 61766413 is a GEOGCS but has three axis rather than twin axis
2. Documentation for WKT says the directions are NORTH | SOUTH | EAST | WEST | UP | DOWN | OTHER
  In epsg.properties there are the following WKTs with 
    AXIS["Geocentric X", GEOCENTRIC_X] 
    AXIS["Plant East", NORTH_EAST]
    AXIS["Plant North", NORTH_WEST]
3. At line 5591 of epsg.properties removed two lines of comments
