# geotrellis-gdal

> GDAL is a translator library for raster and vector geospatial data
> formats
- [gdal.org](http://gdal.org/)

GDAL supports reading and writing in a plethora of
[raster](http://gdal.org/formats_list.html) and
[vector](http://gdal.org/ogr_formats.html) formats. And, while GeoTrellis
supports a few of the most popular, there's currently no plan to achieve
format parity with GDAL. If you'd like to work with an esoteric format
and GDAL supports it though, you're in luck: `geotrellis.gdal` leans on
GDAL to effectively extend the formats with which GeoTrellis can work.  

NOTE: Using the `geotrellis.gdal` module requires a working installation of
GDAL's java bindings.

#### Example

In the following example, we will look at the code necessary to attempt
a read of an [HDF5](http://gdal.org/frmt_hdf5.html) raster band. The
result of a successful reading is an tuple of the form
`(geotrellis.raster.Tile, geotrellis.raster.RasterExtent)`.

```scala
import geotrellis.gdal._

val firstBand: (Tile, RasterExtent) =
  GdalReader.read(path = "/path/to/my/file.he5", band = 1)
```

Note that the `band` parameter specifies the band to be read and turned
into a tile rather than the number of bands expected.

