"""
Converts between GDAL and python rasters.
"""
import sys, os
import math
import json
import log
from raster import GdalLayer
from datatypes import *

def convert(inputPath, 
            output_path, 
            data_type = None,
            band = 1, 
            layer_name = None,
            verify = True,
            rows_per_tile = None,
            cols_per_tile = None):
    layer = GdalLayer(inputPath, band)

    log.notice("Loading raster with width %s, height %s" %
              (layer.raster_extent.cols, layer.raster_extent.rows))

    # Can't process to regular arg if size is greater than
    # java array
    if layer.raster_extent.cols * layer.raster_extent.rows > 2**31:
        log.error('Size (%s) too big for standard arg. '\
              'Use the "--rows-per-tile" and "--cols-per-tile" options'\
              'to create a tiled raster instead')

    if data_type is None:
        data_type = layer.datatype

    if to_struct_fmt(data_type) is None or \
       to_datatype_str(layer.band.DataType) is None:
        log.error('Could not determine datatype')

    if not layer_name:
        layer_name = '.'.join(layer.path.split('.')[:-1])

    if rows_per_tile and cols_per_tile:
        # metadata = getmetadata(layer.dataset, layer.band, data_type)
        # metadata['layer'] = layer_name

        if os.path.isfile(output_path):
            log.error('Output path %s is a file.' % output_path)

        # Check if output is ready for files
        tile_dir = os.path.join(output_path, layer_name)
        if os.path.exists(tile_dir):
            log.error('Output directory %s already exists' % tile_dir)


        tile_row_size = rows_per_tile
        tile_col_size = cols_per_tile
        ntilerows = int(math.ceil(layer.band.YSize / tile_row_size))
        ntilecols = int(math.ceil(layer.band.XSize / tile_col_size))

        nrows = ntilerows * tile_row_size
        ncols = ntilecols * tile_col_size

        metadata = layer.toMap()
        tile_metadata = metadata.copy()

        tile_metadata['type'] = 'tiled'
        tile_metadata['layer'] = layer_name
        tile_metadata['path'] = output_path

        tile_metadata['tile_base'] = layer_name
        tile_metadata['layout_cols'] = str(ntilecols)
        tile_metadata['layout_rows'] = str(ntilerows)
        tile_metadata['pixel_cols'] = str(tile_col_size)
        tile_metadata['pixel_rows'] = str(tile_row_size)
        tile_metadata['cols'] = str(ncols)
        tile_metadata['rows'] = str(nrows)

        json_path = os.path.join(output_path,'%s.json' % layer_name)

        with file(json_path,'w') as mdf:
            mdf.write(
                json.dumps(tile_metadata,
                           sort_keys=True,
                           indent=4,
                           separators=(',',': ')))

        os.makedirs(tile_dir)

        tile_name_ft = '%s_%%d_%%d' % layer_name
        tile_path_ft = os.path.join(tile_dir,'%s.arg' % tile_name_ft)

        total_tiles = ntilerows * ntilecols

        for row in xrange(0,ntilerows):
            ystart = row * tile_row_size
            yend = ystart + tile_row_size
            ywin = [ystart,yend]

            for col in xrange(0,ntilecols):
                tileindex = col + row*ntilecols
                p = int(tileindex * 100.0 / total_tiles)

                sys.stdout.write('%d/%d (%d%%) completed\r' %
                                 (tileindex,total_tiles,p))
                sys.stdout.flush()


                xstart = col * tile_col_size
                xend = xstart + tile_col_size
                xwin = [xstart,xend]

                filename = tile_path_ft % (col,row)

                newmetadata = metadata.copy()
                # Shift xmin and recalculate xmax
                newmetadata['layer'] = tile_name_ft % (col,row)
                newmetadata['xmin'] += xstart*metadata['cellwidth']
                newmetadata['ymin'] += ystart*metadata['cellheight']

                newmetadata['xmax'] = newmetadata['xmin'] + \
                                      metadata['cellwidth']*tile_col_size

                newmetadata['ymax'] = newmetadata['ymin'] + \
                                      metadata['cellheight']*tile_row_size

                newmetadata['rows'] = tile_row_size
                newmetadata['cols'] = tile_col_size

                layer.write_arg(filename, window = (xstart,ystart,xend,yend), verify = verify)

                metadata_file = output_path[0:-4] + '.json'
                with file(metadata_file,'w') as mdf:
                    mdf.write(
                        json.dumps(newmetadata,
                                   sort_keys=True,
                                   indent=4,
                                   separators=(',',': ')))

        print '%s/%s (100%%) completed' % (total_tiles,total_tiles)

    else:
        if os.path.isdir(output_path):
            output_path = os.path.join(output_path, layer_name + '.arg')
        elif not output_path.endswith('.arg'):
            output_path += '.arg'

        metadata_file = output_path[0:-4] + '.json'

        layer.write_arg(output_path, data_type, verify = verify)
        layer.write_metadata(metadata_file, layer_name)
