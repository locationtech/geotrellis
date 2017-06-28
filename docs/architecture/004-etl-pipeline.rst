0004 - ETL Pipelne
------------------

Context
^^^^^^^

The current GeoTrellis ETL does not allow us to determine ETL as a pipeline of transformations / actions.
This document describes a new approach(inspired by [PDAL Pipeline](https://www.pdal.io/pipeline.html))
with a new ETL JSON description.

Decision
^^^^^^^^

We can divide the current ETL into the following steps:

* Load
* Reproject
* Tile
* Pyramid
* Render
* Save

It is possible to represent all these steps as JSON objects, and an array of these objects would be an ETL instruction pipeline.
There still would be three different json inputs: ``input.json``, ``output.json`` and ``backend-profiles.json`` as it seems to be
a reasonable way to divide parameters semantically.


input.json
^^^^^^^^^^

The input schema would be mostly without changes. The only difference would be in the field ``name``,
which should be added as a CLI argument, as an override option. That would allow us to skip some unnecessary
machinery, in the case where all inputs have the same name (handled as a single layer). Another option,
is to have the ``name`` field as an optional field of a ``{save | update | reindex}`` pipeline step.


backend-profiles.json
^^^^^^^^^^^^^^^^^^^^^

Without significant changes, as it already provides minimal information about backends credentials.

output.json
^^^^^^^^^^^

* Reproject
* Tile
* Pyramid
* Render
* Save | Update | Reindex

*Reproject definition:*

.. code:: javascript

  {
    "type": "reproject",
    "method": "{buffered | per-tile}",
    "crs": "{EPSG code | EPSG name | proj4 string}"
  }

* *method* — ``{buffered | per-tile}`` reproject methods
* *crs* — ``{EPSG code | EPSG name | proj4 string}`` destination CRS

*Tile definition:*

.. code:: javascript

 {
    "type": "tile",
    "maxZoom": 19,
    "tileSize": 256,
    "resampleMethod": "{nearest-neighbor | bilinear | cubic-convolution | cubic-spline | lanczos}",
    "layoutScheme": "zoomed",
    "keyIndexMethod": {
      "type": "{zorder | hilbert}",
      "temporalResolution": 86400000
    },
    "cellSize": {
      "width": 0.5,
      "height": 0.5
    },
    "partitions": 5000 // optional // tile into some layout scheme
  }

* *maxZoom* — max zoom level [optional field]
* *tileSize* — destination tile size [optional field]
* *resampleMethod* — ``{nearest-neighbor | bilinear | cubic-convolution | cubic-spline | lanczos}`` methods are possible
* *keyIndexMethod:*
    * *type* — ``{zorder | hilbert}``
    * *temporalResolution* — temporal resolution in ms, if specified it would be a temporal index [optional field]
* *cellSize* — cellSize [optional field]
* *partitions* — partitions number after tiling

*Pyramid definition:*

.. code:: javascript

  {
    "type": "pyramid"
  }

*Render definition:*

.. code:: javascript

  {
    "type": "render",
    "format": "{tiff | png}",
    "path": "{path | pattern}"
  }

* *format* — ``{tiff | png}`` supported formats
* *path* — ``{path | pattern}`` output path, can be specified as a pattern

*{Save | Update | Reindex} definition:*

.. code:: javascript

  {
    "type": "{save | update | reindex}",
    "name": "layer name",
    "backend": {
      "path": "path or table",
      "profile": "profile name"
    }
  }

* *name* — layer name, all inputs would be saved / updated / reindexed with that name
* *backend:*
    * *path* — path or table name
    * *profile* — profile name, can be specified in the ``backend-profiles.json``, default profiles available: ``{file | hadoop | s3}``

*Pipeline example:*

.. code:: javascript

  [
    {
      "type": "reproject",
      "method": "{buffered | per-tile}",
      "crs": "{EPSG code | EPSG name | proj4 string}",
      "cellSize": {
        "width": 0.5,
        "height": 0.5
      }
    },
    {
      "type": "tile",
      "maxZoom": 19,
      "tileSize": 256,
      "resampleMethod": "bilinear",
      "layoutScheme": "zoomed",
      "keyIndexMethod": {
        "type": "zorder",
        "temporalResolution": 86400000
      },
      "partitions": 5000
    },
    {
      "type": "pyramid"
    },
    {
      "type": "render",
      "format": "{tiff | png}",
      "path": "{path | pattern}"
    },
    {
      "type": "{save | reindex | update}",
      "name": "layer name",
      "backend": {
        "path": "path or table",
        "profile": "profile name"
      }
    }
  ]

Conclusion
^^^^^^^^^^

The current ``input.json`` and ``backend-profiles.json`` are seems to be already fine. Significant changes should be introduced
into ``output.json`` as specified above. That would allow us to construct Pipelines similar to what PDAL allows. In addition,
such an approach allows us to not have complicated API extensions, which can be implemented just by implementing desired
pipeline steps functions.
