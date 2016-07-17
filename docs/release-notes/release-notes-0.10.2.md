GeoTrellis 0.10.2, a bugfix release of the 0.10 series, is released. Here are the changes that are included:

- [PR #1561](https://github.com/geotrellis/geotrellis/pull/1561) Fix to polygon sequence union, account that it can result in NoResult. (@lossyrob)
- [PR #1585](https://github.com/geotrellis/geotrellis/pull/1585) Removed warnings; add proper subtyping to GetComponent and SetComponent identity implicits; fix jai travis breakage. (@lossyrob)
- [PR #1569](https://github.com/geotrellis/geotrellis/pull/1569) Moved RDDLayoutMergeMethods functionality to object. (@lossyrob)
- [PR #1494](https://github.com/geotrellis/geotrellis/pull/1494) Add ETL option to specify upper zoom limit for raster layer ingestion (@mbertrand)
- [PR #1571](https://github.com/geotrellis/geotrellis/pull/1571) Fix scallop upgrade issue in spark-etl (@pomadchin)
- [PR #1543](https://github.com/geotrellis/geotrellis/pull/1543) Fix to Hadoop LayerMover (@pomadchin)

Special thanks to new contributor @mbertrand!
