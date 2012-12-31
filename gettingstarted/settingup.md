---
layout: gettingstarted
title: Setting Up GeoTrellis

tutorial: gettingstarted
num: 2
---

### Configuration
GeoTrellis has two important configuration files:

 1. application.conf: defines most system parameters.
 2. catalog.json: defines what geospatial data is available.

#### Application configuration

The main application configuration defines important system parameters used by
your application. Some are required by GeoTrellis, while a particular
application might introduce others.

The configuration file should be on the classpath of your application. When
using SBT, the file is often located at `src/main/resources/application.conf`.

You can also override individual configuration parameters with Java system
properties, e.g. `java -Dgeotrellis.port=5555 ...`

Example:

    // server address to listen on
    geotrellis.host = "0.0.0.0"   
    
    // server port to listen on
    geotrellis.port = 8888                 
    
    // package to search for REST services
    geotrellis.rest-package = "myapp.rest" 
    
    // location for temporary files
    geotrellis.tmp = "/tmp"                

#### Data catalog

The data catalogs is a JSON file containing information about the data
available to GeoTrellis. The catalog is made up of data stores from which data
can be loaded, and also can contain additional information about individual
layers. 

For example, the following `catalog.json` file defines a data directory from
which any .arg files with accompanying .json metadata will be loaded.


    {
      "catalog": "my-catalog",
      "stores": [
      {
        "store": "data:fs",
        "params": {
          "type": "fs",
          "path": "/var/myapp/data"
        }
      }
     ]
    }

NOTE: GeoTrellis will attempt to parse any .json files in the directory specified by the `path` property as raster metadata. You must keep your `catalog.json` file *outside* of this directory.

### Importing Raster Data

GeoTrellis uses a custom raster format, ARG (Azavea Raster Grid), to encode
raster data. This format is maximally simple to read and write, and is designed
for efficient random access.

The most recent development version of GDAL (http://www.gdal.org/) can convert
files from most raster formats into the ARG format. This is by far the best way
to convert data to the ARG format that GeoTrellis uses for raster data.

Please feel free to ask for advice on the mailing list on installing a recent
version of GDAL. There is also an AMI Ubuntu instance for EC2 with GeoTrellis
and a recent version of GDAL installed.  
