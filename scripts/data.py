#!/usr/bin/env python
import argparse
import sys
import gdal
import struct
import math
import array
import json

from gdalconst import *

def error(m,code=1):
    print 'ERROR: %s' % m
    exit(code)

def notice(m):
    print 'NOTICE: %s' % m

def to_datatype_str(n):
    if n == GDT_Byte:
        return 'B'
    elif n == GDT_CInt16 or n == GDT_Int16:
        return 'h'
    elif n == GDT_CInt32 or n == GDT_Int32:
        return 'i'
    elif n == GDT_UInt16:
        return 'H'
    elif n == GDT_UInt32:
        return 'I'
    elif n == GDT_Float32 or n == GDT_CFloat32:
        return 'f'
    elif n == GDT_Float64 or n == GDT_CFloat64:
        return 'd'
    else:
        return None

def to_struct_fmt(n):
    if n == 'bit':
        return 'bit'
    elif n == 'int8':
        return 'b'
    elif n == 'int16':
        return 'h'
    elif n == 'int32':
        return 'i'
    elif n == 'int64':
        return 'q'
    elif n == 'float32':
        return 'f'
    elif n == 'float64':
        return 'd'
    else:
        return None

def nodata_for_fmt(n):
    if n == 'bit':
        return 0
    elif n == 'int8':
        return -2**7
    elif n == 'int16':
        return -2**15
    elif n == 'int32':
        return -2**31
    elif n == 'int64':
        return -2**64
    elif n == 'float32':
        return float('nan')
    elif n == 'float64':
        return float('nan')
    else:
        return None

def create_scanline_transformer(raster):
    inputdt = raster.DataType
    fchar = to_datatype_str(inputdt)
    if fchar is None:
        error('Invalid format ID number: %s' % raster.DataType)

    unpack_str = '>' + (fchar * raster.XSize)

    def f(y):
        scanline = raster.ReadRaster(
            0, y, raster.XSize, 1, raster.XSize, 1, inputdt)

        return struct.unpack(unpack_str, scanline)

    return f

def truncate_pow(exp, val):
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

def convert(args):
    #TODO: Split up this big long method
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

    # Can't process to regular arg if size is greater than
    # java array
    if size > 2**31 and not args.tiled:
        error('Size (%s) too big for standard arg. '\
              'Use the "--tiled" option instead')

    datatype = to_datatype_str(band.DataType)

    if not datatype:
        error('Could not determine datatype')

    ndv = band.GetNoDataValue()
    arg_no_data = nodata_for_fmt(args.data_type)

    ysize = band.YSize
    xsize = band.XSize
    xmin,xres,rot1,ymin,rot2,yres = dataset.GetGeoTransform()

    if rot1 != 0.0 or rot2 != 0.0:
        error('Rotation is not supported')

    xmax = xmin + xsize*xres
    ymax = ymin + ysize*yres

    # Since xres and yres can be negative,
    # we simply use min/max to select the proper bounding
    # box
    metadata = {
        "layer": '.'.join(args.input.split('.')[:-1]),
        "type": "arg",
        "datatype": args.data_type,
        "xmin": min(xmin,xmax),
        "ymin": min(ymin,ymax),
        "xmax": max(xmin,xmax),
        "ymax": max(ymin,ymax),
        "cellwidth": abs(xres),
        "cellheight": abs(yres),
        "rows": ysize,
        "cols": xsize
    }

    if args.metadata:
        for i in metadata.iteritems():
            print "%s: %s" % i
        return

    output_base = args.output

    if output_base.endswith('.arg'):
        metadata_file = output_base[0:-4] + '.json'
        output_file = output_base
    else:
        notice('Appending ".arg" to output name')
        metadata_file = output_base + '.json'
        output_file = output_base + '.arg'

    with file(metadata_file,'w') as mdf:
        mdf.write(json.dumps(metadata,
                             sort_keys=True,
                             indent=4,
                             separators=(',',': ')))

    if args.tiled:
        #TODO: Support tiling
        error('Tiling not yet supported')

    in_scanner = create_scanline_transformer(band)
    writer = create_writer(args.data_type)

    psize = int(ysize / 100)

    output_file = file(args.output, 'wb')
    should_verify = not args.no_verify

    for y in xrange(0,ysize):
        if y % psize == 0:
            sys.stdout.write('%d/%d (%d%%) completed\r' % (y,ysize,y*100/ysize))
            sys.stdout.flush()
            output.flush()

        ar = in_scanner(y)

        # Replace nodata before verification
        data = array.array(to_datatype_str(band.DataType), ar)
        for i in xrange(0,len(data)):
            if data[i] == ndv:
                data[i] = arg_no_data

        writer(output, data, should_verify)

    output.flush()
    output.close()

    print "%d/%d (100%%) completed" % (ysize,ysize)

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
                                     "range (just truncate)")
    convert_parser.add_argument('--tiled',
                                help='Instead of converting to arg, convert '\
                                     'into tiled arg format',
                                action='store_true')
    convert_parser.add_argument('input',
                                help='Name of the input file')
    convert_parser.add_argument('output',
                                help='Output file to write')
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
