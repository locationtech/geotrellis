#!/usr/bin/env python
import argparse
import sys
import gdal
import osr
import struct
import math
import array
import json
import os

from gdalconst import *

# Cheap and easy logging
# probably should use python logging
def error(m,code=1):
    print 'ERROR: %s' % m
    exit(code)

def notice(m):
    print 'NOTICE: %s' % m

# Convert between gdal datatypes
# and python struct package format strings
gdt_datatype_map = {
    GDT_Byte: 'B',
    GDT_CInt16: 'h',
    GDT_Int16: 'h',
    GDT_CInt32: 'i',
    GDT_Int32: 'i',
    GDT_UInt16: 'H',
    GDT_UInt32: 'I',
    GDT_Float32: 'f',
    GDT_CFloat32: 'f',
    GDT_Float64: 'd'
}

inp_datatype_map = {
    'bit': 'bit',
    'int8': 'b',
    'int16': 'h',
    'int32': 'i',
    'int64': 'q',
    'float32': 'f',
    'float64': 'd'
}

nodata_map = {
    'bit': 0,
    'int8': -2**7,
    'int16': -2**15,
    'int32': -2**31,
    'int64': -2**63,
    'float32': float('nan'),
    'float64': float('nan')
}

def to_datatype_str(n):
    """ Convert from integer GDAL datatypes to
        python struct format strings """
    return gdt_datatype_map.get(n, None)

def to_struct_fmt(n):
    """ Convert between input datatypes (int8, float32, etc)
        to python struct format strings """
    return inp_datatype_map.get(n, None)

def nodata_for_fmt(n):
    """ Convert between input datatypes (int8, float32, etc)
        to python struct format strings """
    return nodata_map.get(n, None)

def create_scanline_transformer(raster):
    """ Return a rater reading function
    Param: raster GDAL/SWIG wrapper around a given raster
    Return: A new function with a signature:
       reader(y,x=0,size=None)
    Where:
       y is the offset row to scan
       x is the x offset
       size is the number of cells to read
       the return value is a tuple of cells

    Example:
    t = create_scanlione_transform(raster)

    Get first row:
    row = t(0)

    Get first 10 cells in the 40th row
    cells = t(39,0,10)
    """
    inputdt = raster.DataType
    fchar = to_datatype_str(inputdt)
    if fchar is None:
        error('Invalid format ID number: %s' % raster.DataType)

    def f(y,xoff=0,xsize=None):
        # Network Byte Order (Big Endian)
        if xsize is None:
            xsize = raster.XSize

        unpack_str = '>' + (fchar * xsize)

        scanline = raster.ReadRaster(
            xoff, y, xsize, 1, xsize, 1, inputdt)

        return struct.unpack(unpack_str, scanline)

    return f

def truncate_pow(exp, val):
    """ Given a signed interger datatype with 'exp' bits,
    truncate val so that it is in the interval:
        -2**(exp-1) <= val <= 2**(exp-1) - 1
    """
    if math.isnan(val):
        return -2**(exp-1)

    val = int(val)
    if val >= 2**(exp-1) - 1:
        return 2**(exp -1) - 1
    elif val < -2**(exp - 1):
        return -2**(exp - 1)
    else:
        return val

def get_truncator(outputdt):
    """ Return a function that is able to truncate data
        based on the input type.

        outputdt should be a user input type such as 'bit',
        'int8', etc"""
    if outputdt == 'bit':
        return lambda val: int(val) & 0x1
    elif outputdt == 'int8':
        return lambda val: truncate_pow(8, val)
    elif outputdt == 'int16':
        return lambda val: truncate_pow(16, val)
    elif outputdt == 'int32':
        return lambda val: truncate_pow(32, val)
    elif outputdt == 'int64':
        return lambda val: truncate_pow(64, val)
    else:
        return lambda val: val


def create_writer(outputdt):
    """ Create an arg writing function based on the
    a given input datatype (such as bit, int8, float32, etc)

    Returns a function:
       writer(buffer, values, verify)
    Where:
       buffer is a file-like object with a method write(bytearray)
       values is a list-like structure with the data to write
       verify is a boolean. If 'verify' is set to true and
          data would be truncated, the operation will stop
          If 'verify' is set to false the write operation
          will truncate all datavalues to fit into the given
          datatype
    """
    sfmt = to_struct_fmt(outputdt)
    truncate = get_truncator(outputdt)

    if not sfmt:
        error("Couldn't find a python format for '%s'" % outputdt)

    def f(buffer, values, verify):
        outputfmt = '>' + (sfmt * len(values))
        try:
            buffer.write(struct.pack(outputfmt, *values))
        except Exception, e:
            if verify:
                for v in values:
                    #TODO: Handle bit types specially

                    # Pack and unpack, see if we get the same value
                    failed = False
                    nv = None
                    try:
                        nv = struct.unpack('>' + sfmt,
                                           struct.pack('>' + sfmt, v))[0]
                    except struct.error, e:
                        print e
                        failed = True

                    # Note, is both nv and v are nan, check will fail:
                    # nan != nan, but in our case we want that to be
                    # true so we explicitly check for it here
                    if failed or \
                       (nv != v and
                        (not math.isnan(nv) and math.isnan(v))):
                        print
                        error('Verification failed. Trying to '\
                              'convert to %s resuled in: %s -> %s' %\
                              (outputdt, v, nv))
            else: # Just make it work
                for i in xrange(0,len(values)):
                    buffer.write(struct.pack('>' + sfmt, truncate(values[i])))

    return f

def get_epsg(raster):
    """ Get the EPSG code from a raster or quit if there is an error """
    sr = osr.SpatialReference(raster.GetProjection())
    sr.AutoIdentifyEPSG()

    auth = sr.GetAttrValue('AUTHORITY',0)
    epsg = sr.GetAttrValue('AUTHORITY',1)

    if auth is None or epsg is None or auth.lower() != 'epsg':
        error('Could not get EPSG projection')

    return epsg

def getmetadata(dataset, band, datatype):
    """ Given a raster return a dictionary of useful metadata """

    epsg = get_epsg(dataset)

    ysize = band.YSize
    xsize = band.XSize
    xmin,xres,rot1,ymin,rot2,yres = dataset.GetGeoTransform()

    if rot1 != 0.0 or rot2 != 0.0:
        #TODO: Support rotation?
        error('Rotation is not supported')

    xmax = xmin + xsize*xres
    ymax = ymin + ysize*yres

    # Since xres and yres can be negative,
    # we simply use min/max to select the proper bounding
    # box
    metadata = {
        'type': 'arg',
        'datatype': datatype,
        'xmin': min(xmin,xmax),
        'ymin': min(ymin,ymax),
        'xmax': max(xmin,xmax),
        'ymax': max(ymin,ymax),
        'cellwidth': abs(xres),
        'cellheight': abs(yres),
        'rows': ysize,
        'cols': xsize,
        'xskew': 0,
        'yskew': 0,
        'epsg': epsg
    }

    return metadata

def get_arg_name(output_base):
    """ Given an output name for arg files return
        a tuple with the json and arg names

        If output doesn't end in '.arg' it will be appended"""
    if output_base.endswith('.arg'):
        metadata_file = output_base[0:-4] + '.json'
        output_file = output_base
    else:
        notice('Appending ".arg" to output name')
        metadata_file = output_base + '.json'
        output_file = output_base + '.arg'

    return (output_file, metadata_file)


def write_arg_metadata(metadata, output_name):
    """ Write metadata for a given arg """
    # Write json metadata file. Since the files are pretty
    # sparse we write them in a human-readable fashion
    _, json_file = get_arg_name(output_name)

    with file(json_file,'w') as mdf:
        mdf.write(
            json.dumps(metadata,
                       sort_keys=True,
                       indent=4,
                       separators=(',',': ')))


def write_band_as_arg(metadata, band, output_name, verify,
                      xwin=None, ywin=None,printprg=True):
    """ Write a specific band as arg data
    Parameters:
      metadata: All of the metadata needed to write the json file
         Required keys: datatype
      band: GDAL/SWIG wrapper for a given raster band
      output_name: Name/path of file to write
      verify: True to verify datatype conversion
      xwin (optional): 2-tuple of x start/x end
      ymin (optional): 2-tuple of y start/y end

      set printprg to False to disable debugging output
    """
    outputdt = metadata['datatype']
    arg_file, _ = get_arg_name(output_name)

    # Process the windows for clipping
    if xwin is None:
        xwin = [0,band.XSize]
    if ywin is None:
        ywin = [0,band.YSize]

    if len(ywin) != 2 or len(xwin) != 2:
        error('Invalid window: x in %s, y in %s' % (xwin, ywin))

    xstart = xwin[0]
    xlen = xwin[1] - xwin[0]

    ystart, yend = ywin

    ndv = band.GetNoDataValue()
    arg_no_data = nodata_for_fmt(outputdt)

    in_scanner = create_scanline_transformer(band)
    writer = create_writer(outputdt)

    ysize = yend - ystart
    psize = int(ysize / 100)

    output = file(arg_file, 'wb')

    for y in xrange(ystart,yend):
        if printprg and y % psize == 0:
            yp = y - ystart
            sys.stdout.write('%d/%d (%d%%) completed\r' % (yp,ysize,yp*100/ysize))
            sys.stdout.flush()
            output.flush()

        ar = in_scanner(y,xstart,xlen)

        # Replace nodata before verification
        data = array.array(to_datatype_str(band.DataType), ar)
        for i in xrange(0,len(data)):
            if data[i] == ndv:
                data[i] = arg_no_data

        writer(output, data, verify)

    output.flush()
    output.close()

    if printprg:
        print "%d/%d (100%%) completed" % (ysize,ysize)




def convert(args):
    if not args.metadata and args.output is None:
        error("You need to specify an output location")

    #TODO: Testing
    dataset = gdal.Open(args.input, GA_ReadOnly)

    bands = dataset.RasterCount
    width = dataset.RasterXSize
    height = dataset.RasterYSize
    size = width * height

    if args.data_type == 'bit':
        #TODO: Support bit types
        error('bit datatype not yet supported')

    notice("Loading raster with width %s, height %s, %s band(s)" %
           (width, height, bands))

    if bands == 0:
        error("No bands found in the raster")
    elif bands == 1:
        band = dataset.GetRasterBand(1)
    else:
        #TODO: Support multiband rasters
        error("More than one band not yet supported")

    if (args.rows_per_tile and not args.cols_per_tile) or \
       (args.cols_per_tile and not args.rows_per_tile):
        error('You must specifiy both --rows-per-tile and '\
              '--cols-per-tile or neither')

    # Can't process to regular arg if size is greater than
    # java array
    if size > 2**31:
        error('Size (%s) too big for standard arg. '\
              'Use the "--rows-per-tile" and "--cols-per-tile" options'\
              'to create a tiled raster instead')

    if to_struct_fmt(args.data_type) is None or \
       to_datatype_str(band.DataType) is None:
        error('Could not determine datatype')

    metadata = getmetadata(dataset, band, args.data_type)

    if args.metadata:
        for i in metadata.iteritems():
            print "%s: %s" % i

        return

    if args.name:
        layer_name = args.name
    else:
        layer_name = '.'.join(args.input.split('.')[:-1])

    metadata['layer'] = layer_name

    if args.rows_per_tile and args.cols_per_tile:
        output = args.output
        tile_row_size = args.rows_per_tile
        tile_col_size = args.cols_per_tile
        ntilerows = int(math.ceil(band.YSize / tile_row_size))
        ntilecols = int(math.ceil(band.XSize / tile_col_size))

        nrows = ntilerows * tile_row_size
        ncols = ntilecols * tile_col_size

        tile_metadata = {
            'layer': metadata['layer'],

            'type': 'tiled',
            'datatype': metadata['datatype'],

            'xmin': metadata['xmin'],
            'ymin': metadata['ymin'],
            'xmax': metadata['xmin'] + metadata['cellwidth']*ncols,
            'ymax': metadata['ymin'] + metadata['cellheight']*nrows,

            'cellwidth': metadata['cellwidth'],
            'cellheight': metadata['cellheight'],

            'tile_base': metadata['layer'],
            'layout_cols': ntilecols,
            'layout_rows': ntilerows,
            'pixel_cols': tile_col_size,
            'pixel_rows': tile_row_size,

            'cols': ncols,
            'rows': nrows,

            'xskew': 0,
            'yskew': 0,
            'epsg': metadata['epsg']
        }

        # Check if output is ready for files
        arg_path = os.path.join(output, metadata['layer'])
        if os.path.exists(arg_path):
            error('Output directory already exists')

        os.makedirs(arg_path)
        json_path = os.path.join(args.output,'%s.json' % metadata['layer'])

        with file(json_path,'w') as mdf:
            mdf.write(
                json.dumps(tile_metadata,
                           sort_keys=True,
                           indent=4,
                           separators=(',',': ')))

        tile_name_ft = os.path.join(arg_path,'%s_%%d_%%d.arg' % metadata['layer'])

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

                filename = tile_name_ft % (col,row)

                newmetadata = metadata.copy()
                # Shift xmin and recalculate xmax
                newmetadata['xmin'] += xstart*metadata['cellwidth']
                newmetadata['ymin'] += ystart*metadata['cellheight']

                newmetadata['xmax'] = newmetadata['xmin'] + \
                                      metadata['cellwidth']*tile_col_size

                newmetadata['ymax'] = newmetadata['ymin'] + \
                                      metadata['cellheight']*tile_row_size

                newmetadata['rows'] = tile_row_size
                newmetadata['cols'] = tile_col_size

                write_band_as_arg(newmetadata, band, filename,
                                  not args.no_verify, xwin, ywin,
                                  printprg=False)
                write_arg_metadata(newmetadata, filename)

        print '%s/%s (100%%) completed' % (total_tiles,total_tiles)



    else:
        write_band_as_arg(metadata, band, args.output,
                          not args.no_verify)
        write_arg_metadata(metadata, args.output)


def catalog():
    #TODO: Catalog stuff
    error('Catalog not yet supported')

def main():
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers()

    datatypes = ['bit','int8','int16','int32','int64','float32','float64']

    convert_parser = subparsers.add_parser('convert')
    convert_parser.add_argument('--metadata',
                                action='store_true',
                                help='Print metadata and quit')
    convert_parser.add_argument('--data-type',
                                help='Arg data type. Selecting "auto" will convert '\
                                     'between whatever the input datatype is. '\
                                     'Since unsigned types are not supported '\
                                     'they will automatically be promoted to '\
                                     'the next highest highest signed type.',
                                choices=datatypes,
                                default='int32')
    convert_parser.add_argument('--no-verify',
                                action='store_true',
                                help="Don't verify input data falls in a given"\
                                     'range (just truncate)')

    convert_parser.add_argument('--name',
                                help='Each layer requires a name for the '\
                                      'metadata. By default the name is '\
                                      'input file without the extension.')

    # File names
    convert_parser.add_argument('input',
                                help='Name of the input file')
    convert_parser.add_argument('output',
                                nargs='?',
                                help='Output file to write')
    # Tiles
    tiles_group = convert_parser.add_argument_group('tiles')
    tiles_group.add_argument('--rows-per-tile',
                             help='Number of rows per tile',
                             type=int)
    tiles_group.add_argument('--cols-per-tile',
                             help='Number of cols per tile',
                             type=int)
    # Bands
    bands_group = convert_parser.add_mutually_exclusive_group()
    bands_group.add_argument('--band',
                             help='A specific band to extract')
    bands_group.add_argument('--all-bands',
                             action='store_true',
                             help='Extract all bands to the output directory')

    convert_parser.set_defaults(func=convert)

    catalog_parser = subparsers.add_parser('catalog')
    catalog_parser.set_defaults(func=catalog)

    args = parser.parse_args()
    args.func(args)


if __name__ == '__main__':
    main()
