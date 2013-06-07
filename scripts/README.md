Getting data into GeoTrellis
=====

There are two main verbs:
- convert
- catalog

Convert
----

Convert can be used to move between GDAL support formats an args:

```
usage: data.py convert [-h] [--metadata]
                       [--data-type {bit,int8,int16,int32,int64,float32,float64}]
                       [--no-verify] [--tiled] [--band BAND | --all-bands]
                       input output

positional arguments:
  input                 Name of the input file
  output                Output file to write

optional arguments:
  -h, --help            show this help message and exit
  --metadata            Print metadata and quit
  --data-type {bit,int8,int16,int32,int64,float32,float64}
                        Arg data type. Selecting "auto" will convert between
                        whatever the input datatype is. Since unsigned types are
                        not supported they will automatically be promoted to the
                        next highest signed type.
  --no-verify           Don't verify input data falls in a given range (just
                        truncate)
  --tiled               Instead of converting to arg, convert into tiled arg format
  --band BAND           A specific band to extract
  --all-bands           Extract all bands to the output directory
```

Catalog
----

Catalog can be used to manage the GeoTrellis catalog.

Generally something like this:

```
$ python data.py catalog create path/to/catalog.json
$ python data.py catalog add-dir path/to/catalog.json dir/with/args
```

You may also find it helpful to update catalog metadata with this tool,
instead of diving into the JSON:

```
$ python data.py catalog update path/to/catalog.json args:fs cacheAll false
```
