package geotrellis.raster.io.geotiff

package object compression extends LZWDecompression
    with PackBitsDecompression
    with ZLibDecompression
