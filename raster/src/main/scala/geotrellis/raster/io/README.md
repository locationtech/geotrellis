# GeoTrellis Raster IO

#### GeoTiff format
"GeoTIFF represents an effort by over 160 different remote sensing, GIS, cartographic, and surveying related companies and organizations to establish a TIFF based interchange format for georeferenced raster imagery." [GeoTiff resources](https://trac.osgeo.org/geotiff/)

#### ARG format
The Azavea Raster Grid (ARG) format was developed by [Azavea](http://www.azavea.com) for persisting raster data. By storing metadata in a separate JSON file and avoiding compression, ARGfiles can be read more quickly than comparable GeoTiffs. GeoTrellis leans more heavily on GeoTiffs now that they are supported (through the ARG package in this directory) and have become the de facto standard in the geospatial community.
