## GeoTiffs

- [Introduction](#introduction)
- [GeoTiff File Format](#geotiff-file-format)
   - [File Header](#file-header)
   - [File Directory](#file-directory)
   - [Image Data](#image-data)
      - [Striped](#striped)
      - [Tiled](#tiled)
    - [Layout of Columns and Rows](#layout-of-columns-and-rows)
- [Further Readings](#further-reading)
- [To Do](#to-do)

- - -
### Introduction
GeoTiffs are a type of Tiff image file that contain image data pertaining to satiellete, arial, and evelation imagery amoung other types of geospatial information. What differentiates them from convential Tiffs is that additional metadata pertaining to the image is embedded wihtin the file. The positions of geographic features on the screen and how they are projected are two such examples of information found within GeoTiffs but lacking in a normal Tiff.
- - -
### GeoTiff File Format
Because GeoTiffs are Tiffs wtih extended features, they have both have the same file structure. There exists three components that can be found in Tiff files: the header, file directory, and the actual image data. Within these files, the directories and image data can be found at any point within the file, regardless of how the images are presented when the file is opended and viewed. The header is the only section which has a constant location and that is at the begining of the file.

#### File Header
As stated earlier, the header is found at the beginning of every Tiff file including GeoTiffs. Due to how the other components can be situated anywhere within the file, the header provides the offset value that points to the first file directory. Without this offset, it would be impossible to read a Tiff file.

#### File Directory
For each piece of image data found in a Tiff there exists a corresponding file directory for that image. The directory itself holds details about the image. Each property listed in the directory is reffered to as a, "Tag". Tags contain information on, but not limited to, the image size, compression types, and the type of color plan. Since we're working with GeoTiffs, important geographical information is also included such as the coordinate system the image is in and the latitudal and longitudal extent of the image. These directories can vary in size, as users can create their own tags and each image in the file does not need to have exact same tags.

Other than image attributes, the file directory holds two offset values that play a role in reading the file. One points to where the actual image itself is located, and the other shows where the the next file directory can be found.

#### Image Data
A Tiff file can store any number of images wihtin a single file, including none at all. In the case of GeoTiffs, the images themselves are almost always stored as bitmap data. It is important to understand that there are two ways in which the image can be stored. These two storage methods are: Striped and Tiled.

##### Striped
As seen in the picture below, the stiped storage format splits each image into long, vertical strips. Contained within them are columns of bitmapped image data. If your GeoTiff file was created before the realse of Tiff 6.0, then this is the data storage method in which it most likely uses.

If an image has strip storage, then it's corresponding file directory contains the tags: RowsPerStrip, StripOffsets, and StripByteCount. All three of these are needed to read the image. The first one is the number of rows that are contained within the strips. Every strip wihtin an image must have the same number of rows within it except for the last one in certain instances. StripOffsets is an array of offsets that shows where each strip starts. The last tag, ByteSegmentCount, is also an array of values that contains the size of each strip in terms of Bytes.

##### Tiled
Tiff 6.0 introduced tiled storage for image data. These rectangluar segments have both a height and a width that must be divisible by 16. There are instances where the tiled grid does not fit the image exactly. When this occurs, padding is added around the image so as to meet the requirement of having each tile having dimensions of a factor of 16. See the image below for a visual representation of what this format looks like.

As with stips, tiles have specific tags that are needed in order to read the image. These new tags are: TileWidth, TileLength, TileOffsets, and TileByteCounts. TileWidth is the number of coloumns and TileLength is the number of rows that are found within the specified tile. As with striped, TileOffsets and TileByteCounts are arrays that contain the begining offset and the byte of each tile in the image respectivly.

#### Layout of Columns and Rows
There exists two ways in which to describe a location in GeoTiffs. One is in Map coordinates which use X and Y values. X's are oriented along the horizontal axis and run from west to east while Y's are on the vertical axis and run from south to north.

When a GeoTiff is read, individual cell are not located with map coordinates, rather, they use a grid coordinate system. The horizontal axis is represented as columns and runs the same way as the map coordinate X value. However, the vertical axis, called Rows, runs north to south.
- - -
### Further Readings
* [More info on the Tiff file format](http://www.fileformat.info/format/tiff/egff.htm)
* [More info on the GeoTiff file format](http://www.gdal.org/frmt_gtiff.html)

- - -
### To Do
Added the pictures of how the image data is layed out.
