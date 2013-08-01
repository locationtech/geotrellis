"""
Converts between GDAL and python rasters.
"""
import sys, os, shutil
import math
import json
import log
from raster import Layer, GdalLayer, Extent
from datatypes import *

def convert(inputPath, 
            output_path, 
            data_type = None,
            band = 1, 
            layer_name = None,
            verify = True,
            rows_per_tile = None,
            cols_per_tile = None,
            legacy = False,
            clobber = False):
    if band != 1:
        layer = GdalLayer(inputPath,band)
    else:
        layer = Layer.fromPath(inputPath)
    layer.init_data()

    raster_extent = layer.raster_extent()

    log.notice("Loading raster with width %s, height %s" %
              (raster_extent.cols, raster_extent.rows))

    # Can't process to regular arg if size is greater than
    # java array
    if raster_extent.cols * raster_extent.rows > 2**31:
        log.error('Size (%s) too big for standard arg. '\
              'Use the "--rows-per-tile" and "--cols-per-tile" options'\
              'to create a tiled raster instead')

    if data_type is None:
        data_type = layer.data_type()

    if to_struct_fmt(data_type) is None:
        log.error('Could not determine datatype')

    if not layer_name:
        layer_name = '.'.join(os.path.basename(inputPath).split('.')[:-1])

    if rows_per_tile and cols_per_tile:
        if os.path.isfile(output_path):
            log.error('Output path %s is a file.' % output_path)

        # Check if output is ready for files
        tile_dir = os.path.join(output_path, layer_name)
        if legacy:
            # GeoTrellis 0.8.x puts the metadata file inside the 
            # tile directory, and names it layout.json
            json_path = os.path.join(tile_dir,'layout.json')
        else:
            json_path = os.path.join(output_path,'%s.json' % layer_name)

        if os.path.exists(tile_dir):
            if clobber:
                shutil.rmtree(tile_dir)
            else:
                log.error('Output directory %s already exists' % tile_dir)

        if os.path.exists(json_path):
            if clobber:
                os.remove(json_path)
            else:
                log.error('File %s already exists' % json_path)

        layer.write_tiled(tile_dir, 
                          json_path, 
                          layer_name, 
                          rows_per_tile,
                          cols_per_tile,
                          data_type = data_type, verify = verify)

        print 'Tile conversion completed.'
    else:
        if os.path.isdir(output_path):
            output_path = os.path.join(output_path, layer_name + '.arg')
        elif output_path.endswith('.json'):
            output_path = output_path[:-5] + '.arg'
        elif not output_path.endswith('.arg'):
            output_path += '.arg'

        metadata_file = output_path[0:-4] + '.json'

        if os.path.exists(output_path):
            if clobber:
                os.remove(output_path)
            else:
                log.error("File %s already exists" % output_path)

        if os.path.exists(metadata_file):
            if clobber:
                os.remove(metadata_file)
            else:
                log.error("File %s already exists" % metadata_file)

        layer.write_arg(output_path, data_type = data_type, verify = verify)
        layer.write_metadata(metadata_file, output_path, layer_name, data_type)
    layer.close()
