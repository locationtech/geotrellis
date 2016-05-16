from __future__ import absolute_import
from geotrellis.spark.io.file.FileValueReader import file_value_reader
from geotrellis.spark.SpatialKey import SpatialKey
from geotrellis.spark.LayerId import LayerId

def doit(path_to_landsat, zoom = 0, col = 0, row = 0):
    cat = path_to_landsat + '/data/catalog'
    flv = file_value_reader(cat)
    def reader(layerid):
       return flv.reader(SpatialKey, object, layerid)
     
    rdr = reader(LayerId("landsat", zoom))
    return rdr.read(SpatialKey(col, row))

