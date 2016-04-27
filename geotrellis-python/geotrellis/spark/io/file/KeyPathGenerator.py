from geotrellis.spark.io.index.Index import Index
import os.path

def generate_key_path_func(catalog_path, layer_path, key_index, max_width):
    return lambda key: os.path.join(catalog_path, layer_path, Index.encode(key_index.to_index(key), max_width))
    
