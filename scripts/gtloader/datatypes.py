"""
This module deals with raster data types.
"""

from gdalconst import *

class arg:
    datatypes = ['bit','int8','int16','int32','int64','float32','float64']

# Convert between gdal datatypes
# and python struct package format strings
gdt_datatype_map = {
    GDT_Byte:     'B',
    GDT_CInt16:   'h',
    GDT_Int16:    'h',
    GDT_CInt32:   'i',
    GDT_Int32:    'i',
    GDT_UInt16:   'H',
    GDT_UInt32:   'I',
    GDT_Float32:  'f',
    GDT_CFloat32: 'f',
    GDT_Float64:  'd'
}

# Convert between gdal datatypes
# and arg datatypes
gdal_arg_datatype_map = {
    GDT_Byte:     'int8',
    GDT_CInt16:   'int16',
    GDT_Int16:    'int16',
    GDT_CInt32:   'int32',
    GDT_Int32:    'int32',
    GDT_UInt16:   'int32',
    GDT_UInt32:   'int64',
    GDT_Float32:  'float32',
    GDT_CFloat32: 'float32',
    GDT_Float64:  'float64'
}

# Convert between ARG datatypes
# and python struct package format strings
inp_datatype_map = {
    'bit':     'bit',
    'int8':    'b',
    'int16':   'h',
    'int32':   'i',
    'int64':   'q',
    'float32': 'f',
    'float64': 'd'
}

# Maps ARG datatypes to NoData values
nodata_map = {
    'bit':      0,
    'int8':    -2**7,
    'int16':   -2**15,
    'int32':   -2**31,
    'int64':   -2**63,
    'float32': float('nan'),
    'float64': float('nan')
}

def to_datatype_str(n):
    """ Convert from integer GDAL datatypes to
        python struct format strings """
    return gdt_datatype_map.get(n, None)

def to_datatype_arg(n):
    """ Convert from integer GDAL datatypes to
        arg datatypes """
    return gdal_arg_datatype_map.get(n, None)

def to_struct_fmt(n):
    """ Convert between input datatypes (int8, float32, etc)
        to python struct format strings """
    return inp_datatype_map.get(n, None)

def nodata_for_fmt(n):
    """ Convert between input datatypes (int8, float32, etc)
        to python struct format strings """
    return nodata_map.get(n, None)
