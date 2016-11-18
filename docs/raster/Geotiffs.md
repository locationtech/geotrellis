## GeoTiffs

- [Introduction](#introduction)
- [GeoTiff File Format](#geotiff-file-format)
   - [File Header](#file-header)
   - [Image File Directory](#image-file-directory)
   - [Image Data](#image-data)
      - [Striped](#striped)
      - [Tiled](#tiled)
    - [Layout of Columns and Rows](#layout-of-columns-and-rows)
- [Big Tiffs](#big-tiffs)
- [Further Readings](#further-reading)

- - -
### Introduction
GeoTiffs are a type of Tiff image file that contain image data pertaining to
satellite, aerial, and elevation data among other types of geospatial
information. The additional pieces of metadata that are needed to store and
display this information is what sets GeoTiffs apart from normal Tiffs. For
instance, the positions of geographic features on the screen and how they are
projected are two such pieces of data that can be found within a GeoTiff, but
is absent from a normal Tiff file.
- - -

### GeoTiff File Format
Because GeoTiffs are Tiffs with extended features, they both have the same file
structure. There exist three components that can be found in all Tiff files:
the header, the image file directory, and the actual image data. Within these
files, the directories and image data can be found at any point within the file;
regardless of how the images are presented when the file is opened and viewed.
The header is the only section which has a constant location, and that is at the
begining of the file.

#### File Header
As stated earlier, the header is found at the beginning of every Tiff file,
including GeoTiffs. All Tiff files have the exact same header size of 8 bytes.
The first two bytes of the header are used to determine the `ByteOrder` of the
file, also known as "Endianness". After these two, comes the next two bytes
which are used to determine the file's magic number. `.tif`, `.txt`, `.shp`, and
all other file types have a unique identifier number that tells the program kind
of file it was given. For Tiff files, the magic number is 42. Due to how the
other components can be situated anywhere within the file, the  last 4 bytes of
the header provide the offset value that points to the first file directory.
Without this offset, it would be impossible to read a Tiff file.

#### Image File Directory

For every image found in a Tiff file there exists a corresponding image file
directory for that picture. Each property listed in the directory is referred to
as a `Tag`. `Tag`s contain information on, but not limited to, the image size,
compression types, and the type of color plan. Since we're working with
Geotiffs, geo-spatial information is also documented within the `Tag`s. These
directories can vary in size, as users can create their own tags and each image
in the file does not need to have exact same tags.

Other than image attributes, the file directory holds two offset values that
play a role in reading the file. One points to where the actual image itself is
located, and the other shows where the the next file directory can be found.


#### Image Data
A Tiff file can store any number of images within a single file, including none
at all. In the case of GeoTiffs, the images themselves are almost always stored
as bitmap data. It is important to understand that there are two ways in which
the actual image data is formatted within the file. The two methods are: Striped
and Tiled.

##### Striped
Striped storage breaks the image into segments of long, vertical bands that
stretch the entire width of the picture. Contained within them are columns of
bitmapped image data. If your GeoTiff file was created before the realse of Tiff
6.0, then this is the data storage method in which it most likely uses.

If an image has strip storage, then its corresponding file directory contains
the tags: `RowsPerStrip`, `StripOffsets`, and `StripByteCount`. All three of
these are needed to read that given segment. The first one is the number of rows
that are contained within the strips. Every strip within an image must have the
same number of rows within it except for the last one in certain instances.
`StripOffsets` is an array of offsets that shows where each strip starts within
the file. The last tag, `ByteSegmentCount`, is also an array of values that
contains the size of each strip in terms of Bytes.

##### Tiled
Tiff 6.0 introduced a new way to arrange and store data within a Tiff, tiled
storage. These rectangular segments have both a height and a width that must be
divisible by 16. There are instances where the tiled grid does not fit the image
exactly. When this occurs, padding is added around the image so as to meet the
requirement of each tile having dimensions of a factor of 16.

As with stips, tiles have specific tags that are needed in order to process each
segment. These new tags are: `TileWidth`, `TileLength`, `TileOffsets`, and
`TileByteCounts`. `TileWidth` is the number of columns and `TileLength` is the
number of rows that are found within the specified tile. As with striped,
`TileOffsets` and `TileByteCounts` are arrays that contain the begining offset
and the byte count of each tile in the image, respectively.

#### Layout of Columns and Rows
There exists two ways in which to describe a location in GeoTiffs. One is in Map
coordinates which use X and Y values. X's are oriented along the horizontal axis
and run from west to east while Y's are on the vertical axis and run from south
to north. Thus the further east you are, the larger your X value ; and the more
north you are the larger your Y value.

The other method is to use the grid coordinate system. This technique of
measurement uses Cols and Rows to describe the relative location of things. Cols
run east to west whereas Rows run north to south. This then means that Cols
increase as you go east to west, and rows increase as you go north to south.
- - -

### Big Tiffs
In some instances, your GeoTiff may contain an amount of data so large that it
can no longer be described as a Tiff, but rather by a new name, BigTiff. In
order to qualify as a BigTiff, your file needs to be **at least 4gb in size or
larger**. At this point, the methods used to store and find data need to be
changed. The accommodation that is made is to change the size of the various
offsets and byte counts of each segment. For a normal Tiff, this size is
32-bits, but BigTiffs have these sizes at 64-bit. GeoTrellis supports BigTiffs
without any issue, so one need not worry about size when working with
their files.
- - -

### Further Readings
* [For more information on the Tiff file format](http://www.fileformat.info/format/tiff/egff.htm)
* [For more information on the GeoTiff file format](http://www.gdal.org/frmt_gtiff.html)
