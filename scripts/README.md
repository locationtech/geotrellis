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
                        next highest highest signed type.
  --no-verify           Don't verify input data falls in a givenrange (just
                        truncate)
  --tiled               Instead of converting to arg, convert into tiled arg format
  --band BAND           A specific band to extract
  --all-bands           Extract all bands to the output directory
```
