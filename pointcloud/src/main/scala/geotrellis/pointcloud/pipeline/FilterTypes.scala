package geotrellis.pointcloud.pipeline

sealed trait FilterType {
  val name: String

  override def toString = name
}

case object approximatecoplanar extends FilterType { val name = "approximatecoplanar" }
case object attribute extends FilterType { val name = "attribute" }
case object chipper extends FilterType { val name = "chipper" }
case object colorinterp extends FilterType { val name = "colorinterp" }
case object colorization extends FilterType { val name = "colorization" }
case object computerange extends FilterType { val name = "computerange" }
case object crop extends FilterType { val name = "crop" }
case object decimation extends FilterType { val name = "decimation" }
case object divider extends FilterType { val name = "divider" }
case object eigenvalues extends FilterType { val name = "eigenvalues" }
case object estimaterank extends FilterType { val name = "estimaterank" }
case object ferry extends FilterType { val name = "ferry" }
case object greedyprojection extends FilterType { val name = "greedyprojection" }
case object gridprojection extends FilterType { val name = "gridprojection" }
case object hag extends FilterType { val name = "hag" }
case object hexbin extends FilterType { val name = "hexbin" }
case object iqr extends FilterType { val name = "iqr" }
case object kdistance extends FilterType { val name = "kdistance" }
case object lof extends FilterType { val name = "lof" }
case object mad extends FilterType { val name = "mad" }
case object merge extends FilterType { val name = "merge" }
case object mongus extends FilterType { val name = "mongus" }
case object mortonorder extends FilterType { val name = "mortonorder" }
case object movingleastsquares extends FilterType { val name = "movingleastsquares" }
case object normal extends FilterType { val name = "normal" }
case object outlier extends FilterType { val name = "outlier" }
case object pclblock extends FilterType { val name = "pclblock" }
case object pmf extends FilterType { val name = "pmf" }
case object poisson extends FilterType { val name = "poisson" }
case object predicate extends FilterType { val name = "predicate" }
case object programmable extends FilterType { val name = "programmable" }
case object radialdensity extends FilterType { val name = "radialdensity" }
case object range extends FilterType { val name = "range" }
case object randomize extends FilterType { val name = "range" }
case object reprojection extends FilterType { val name = "reprojection" }
case object sample extends FilterType { val name = "sample" }
case object smrf extends FilterType { val name = "smrf" }
case object sort extends FilterType { val name = "sort" }
case object splitter extends FilterType { val name = "splitter" }
case object stats extends FilterType { val name = "stats" }
case object transformation extends FilterType { val name = "transformation" }
case object voxelgrid extends FilterType { val name = "voxelgrid" }

object FilterTypes {
  lazy val all = List(
    approximatecoplanar,
    attribute,
    chipper,
    colorinterp,
    colorization,
    computerange,
    crop,
    decimation,
    divider,
    eigenvalues,
    estimaterank,
    ferry,
    greedyprojection,
    gridprojection,
    hag,
    hexbin,
    iqr,
    kdistance,
    lof,
    mad,
    merge,
    mongus,
    mortonorder,
    movingleastsquares,
    normal,
    outlier,
    pclblock,
    pmf,
    poisson,
    predicate,
    programmable,
    radialdensity,
    randomize,
    range,
    reprojection,
    sample,
    smrf,
    sort,
    splitter,
    stats,
    transformation,
    voxelgrid
  )
}