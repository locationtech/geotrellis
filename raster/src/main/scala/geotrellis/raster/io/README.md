#geotrellis.raster.io

As elsewhere, `geotrellis.raster`'s `io` package defines means of serializing and deserializing its parent package's data for persistance and networking uses. Here are `geotrellis.raster.io`'s packages:

- `geotrellis.raster.io.arg` defines methods for moving data into and out of the [Azavea Raster Grid format](http://geotrellis.io/documentation/0.9.0/geotrellis/io/arg/).
- `geotrellis.raster.io.ascii` defines methods for interacting with ascii-art representations of rasters.
- `geotrellis.raster.io.geotiff` defines tools for reading and writing `.tif` files. This is what you're most likely to find useful. It contains a `reader` and `writer` package.
- `geotrellis.raster.io.json` defines tools for encoding/decoding raster data (typically not the entire raster...) as json.