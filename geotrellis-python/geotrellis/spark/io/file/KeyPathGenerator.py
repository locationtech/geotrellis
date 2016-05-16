from __future__ import absolute_import
from geotrellis.spark.io.index.Index import Index
import os.path

def generate_key_path_func(catalog_path, layer_path, key_index, max_width = None):
    if max_width is None:
        max_width = key_index
        return lambda key: os.path.join(catalog_path, layer_path, max_width)
    else:
        return lambda key: os.path.join(catalog_path, layer_path, Index.encode(key_index.toIndex(key), max_width))
    
