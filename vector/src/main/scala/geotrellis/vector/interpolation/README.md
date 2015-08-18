# [Kriging Interpolation](https://en.wikipedia.org/wiki/Kriging)
This package is to be used for Kriging interpolation.

## [Semivariograms](https://en.wikipedia.org/wiki/Variogram)
This method of interpolation is based on constructing Semivariograms. For grasping the structure of spatial dependencies of the known data-points, semivariograms are constructed.

Firstly, the sample datat-points' spatial structure to be captured are converted to an empirical semivariogram, which is then fitted into explicit/theoretical semivariogram models.

There are two types of Semivariograms developed :

  - Linear Semivariogram
  - Non-Linear Semivariograms

### Empirical Semivariogram
```
//(The array of sample points)
val points: Array[PointFeature[Double]] = ...

/** The empirical semivariogram generation
  * "maxDistanceBandwidth" denotes the maximum inter-point distance relationship
  * that one wants to capture in the empirical semivariogram.
  */
val es: EmpiricalVariogram = EmpiricalVariogram.nonlinear(points, maxDistanceBandwidth, binMaxCount)
```

The sample-data point used for training the Kriging Models are clustered into groups(aka bins) and the data-values associated with each of the data-points are aggregated into the bin's value. There are various ways of constructing the bins, i.e. equal bin-size(same number of points in each of the bins); or equal lag-size(the bins are separated from each other by a certain fixed separation, and the samples with the inter-points separation fall into the corresponding bins).

In case, there are outlier points in the sample data, the equal bin-size approach assures that the points' influence is tamed down; however in the second approach, the outliers would have to be associated with weights(which is computationally more intensive).

The final structure of the empirical variogram has an array of tuples :

    (h, k)
    where h => Inter-point distance separation
          k => The variogram's data-value (used for covariogram construction)
Once the empirical semivariograms have been evaluated, these are fitted into the theoretical semivariogram models (the fitting is carried out into those models which best resemble the empirical semivariogram's curve generate).

### Linear Semivariogram

```
/** "radius" denotes the maximum inter-point distance to be
  * captured into the semivariogram
  * "lag" denotes the inter-bin distance
  */
val points: Array[PointFeature[Double]] = ...
val linearSV = LinearSemivariogram(points, radius, lag)
```
This is the simplest of all types of explicit semivariogram models and does not very accurately capture the spatial structure, since the data is rarely linearly changing.
This consists of the points' being modelled using simple regression into a straight line. The linear semivariogram has linear dependency on the free variable (inter-point distance) and is represented by :

```f(x) = slope * x + intercept```

### Non-Linear Semivariogram

```
/**
  * ModelType can be any of the models from
  * "Gaussian", "Circular", "Spherical", "Exponential" and "Wave"
  */
val points: Array[PointFeature[Double]] = ...
val nonLinearSV: Semivariogram =
    NonLinearSemivariogram(points, 30000, 0, [[ModelType]])
```
Most often the empirical variograms can not be adequately represented by the use of linear variograms. The non-linear variograms are then used to model the empirical semivariograms for use in Kriging intepolations.
These have non-linear dependencies on the free variable (inter-point distance).

In case the empirical semivariogram has been previously constructed, it can be fitted into the semivariogram models by :
```
val svSpherical: Semivariogram =
    Semivariogram.fit(empiricalSemivariogram, Spherical)
```

The popular types of Non-Linear Semivariograms are :

```(h in each of the function definition denotes the inter-point distances)```

#### Gaussian Semivariogram
```
//For explicit/theoretical Gaussian Semivariogram
val gaussianSV: Semivariogram =
    NonLinearSemivariogram(range, sill, nugget, Gaussian)
```
The formulation of the Gaussian model is :

                        | 0                                 , h = 0
    gamma(h; r, s, a) = |
                        | a + (s - a) {1 - e^(-h^2 / r^2)}  , h > 0


#### Circular Semivariogram
```
//For explicit/theoretical Circular Semivariogram
val circularSV: Semivariogram =
    NonLinearSemivariogram(range, sill, nugget, Circular)
```
                          | 0                                                                        , h = 0
                          |
                          |               |                                              _________ |
                          |               |      2                | h |      2h         /    h^2   |
      gamme(h; r, s, a) = | a + (s - a) * |1 - ----- * cos_inverse|---| + -------- *   /1 - -----  | , 0 < h <= r
                          |               |      pi               | r |    pi * r    \/      r^2   |
                          |               |                                                        |
                          |
                          | s                                                                        , h > r
#### Spherical Semivariogram
```
//For explicit/theoretical Spherical Semivariogram
val sphericalSV: Semivariogram =
    NonLinearSemivariogram(range, sill, nugget, Spherical)
```
                        | 0                             , h = 0
                        |             | 3h      h^3   |
    gamma(h; r, s, a) = | a + (s - a) |---- - ------- | , 0 < h <= r
                        |             | 2r     2r^3   |
                        | s                             , h > r

#### Exponential Semivariogram
```
//For explicit/theoretical Exponential Semivariogram
val exponentialSV: Semivariogram =
    NonLinearSemivariogram(range, sill, nugget, Exponential)
```
                        | 0                                  , h = 0
    gamma(h; r, s, a) = |
                        | a + (s - a) {1 - e^(-3 * h / r)}   , h > 0

#### Wave Semivariogram
```
//For explicit/theoretical Exponential Semivariogram
//For wave, range (viz. r) = wave (viz. w)
val waveSV: Semivariogram =
    NonLinearSemivariogram(range, sill, nugget, Wave)
```
                         | 0                                 , h = 0
                         |
     gamma(h; w, s, a) = |             |       sin(h / w)  |
                         | a + (s - a) |1 - w ------------ | , h > 0
                         |             |           h       |

##### Notes on Semivariogram fitting
The empirical semivariogram tuples generated are fitted into the semivariogram models using [Levenberg Marquardt Optimization](https://en.wikipedia.org/wiki/Levenberg%E2%80%93Marquardt_algorithm). This internally uses jacobian (differential) functions corresponding to each of the individual models for finding the optimum range, sill and nugget values of the fitted semivariogram.
```
//For the Spherical model
val model: ModelType = Spherical
valueFunc(r: Double, s: Double, a: Double): (Double) => Double =
    NonLinearSemivariogram.explicitModel(r, s, a, model)
```
The Levenberg Optimizer uses this to reach to the global minima much faster as compared to unguided optimization.

In case, the initial fitting of the empirical semivariogram generates a negative nugget value, then the process is re-run after forcing the nugget value to go to zero (since mathematically, a negative nugget value is absurd).

## Kriging Methods
Once the semivariograms have been constructed using the known point's values, the kriging methods can be invoked.

The methods are largely classified into different types in the way the mean(mu) and the covariance values of the object are dealt with.

    //Array of sample points with given data
    val points: Array[PointFeature[Double]] = ...

    //Array of points to be kriged
    val location: Array[Point] = ...

There exist four major kinds of Kriging interpolation techniques, namely :

#### Simple Kriging

    //Simple kriging, tuples of (prediction, variance) per prediction point
    val sv: Semivariogram = NonLinearSemivariogram(points, 30000, 0, Spherical)
    
    val krigingVal: Array[(Double, Double)] =
        new SimpleKriging(points, 5000, sv)
          .predict(location)
    /**
      * The user can also do Simple Kriging using :
      * new SimpleKriging(points).predict(location)
      * new SimpleKriging(points, bandwidth).predict(location)
      * new SimpleKriging(points, sv).predict(location)
      * new SimpleKriging(points, bandwidth, sv).predict(location)
      */

It is belongs to the class of Simple Spatial Prediction Models.

The simple kriging is based on the assumption that the underlying stochastic process is entirely _known_ and the spatial trend is constant, viz. the mean and covariance values of the entire interpolation set is constant (using solely the sample points)

    mu(s) = mu              known; s belongs to R
    cov[eps(s), eps(s')]    known; s, s' belongs to R

#### Ordinary Kriging
    //Ordinary kriging, tuples of (prediction, variance) per prediction point
    val sv: Semivariogram = NonLinearSemivariogram(points, 30000, 0, Spherical)
    
    val krigingVal: Array[(Double, Double)] =
        new OrdinaryKriging(points, 5000, sv)
          .predict(location)
    /**
      * The user can also do Ordinary Kriging using :
      * new OrdinaryKriging(points).predict(location)
      * new OrdinaryKriging(points, bandwidth).predict(location)
      * new OrdinaryKriging(points, sv).predict(location)
      * new OrdinaryKriging(points, bandwidth, sv).predict(location)
      */

It is belongs to the class of Simple Spatial Prediction Models.

This method differs from the Simple Kriging appraoch in that, the constant mean is assumed to be unknown and is estimated within the model.

    mu(s) = mu              unknown; s belongs to R
    cov[eps(s), eps(s')]    known; s, s' belongs to R

#### Universal Kriging
    //Universal kriging, tuples of (prediction, variance) per prediction point
    
    val attrFunc: (Double, Double) => Array[Double] = {
      (x, y) => Array(x, y, x * x, x * y, y * y)
    }
    
    val krigingVal: Array[(Double, Double)] =
        new UniversalKriging(points, attrFunc, 50, Spherical)
          .predict(location)
    /**
      * The user can also do Universal Kriging using :
      * new UniversalKriging(points).predict(location)
      * new UniversalKriging(points, bandwidth).predict(location)
      * new UniversalKriging(points, model).predict(location)
      * new UniversalKriging(points, bandwidth, model).predict(location)
      * new UniversalKriging(points, attrFunc).predict(location)
      * new UniversalKriging(points, attrFunc, bandwidth).predict(location)
      * new UniversalKriging(points, attrFunc, model).predict(location)
      * new UniversalKriging(points, attrFunc, bandwidth, model).predict(location)
      */

It is belongs to the class of General Spatial Prediction Models.

This model allows for explicit variation in the trend function (mean function) constructed as a linear function of spatial attributes; with the covariance values assumed to be known. This model computes the prediction using

For example if :

    x(s) = [1, s1, s2, s1 * s1, s2 * s2, s1 * s2]'
    mu(s) = beta0 + beta1*s1 + beta2*s2 + beta3*s1*s1 + beta4*s2*s2 + beta5*s1*s2
Here, the "linear" refers to the linearity in parameters (beta).

    mu(s) = x(s)' * beta,   beta unknown; s belongs to R
    cov[eps(s), eps(s')]    known; s, s' belongs to R
    
The `attrFunc` function is the attribute function, which is used for evaluating non-constant spatial trend structures. Unlike the Simple and Ordinary Kriging models which rely only on the residual values for evaluating the spatial structures, the General Spatial Models may be modelled by the user based on the data (viz. evaluating the beta variable to be used for interpolation).

In case the user does not specify an attribute function, by default the function used is a quadratic trend function for Point(s1, s2) :

```mu(s) = beta0 + beta1*s1 + beta2*s2 + beta3*s1*s1 + beta4*s2*s2 + beta5*s1*s2```

General example of a trend function is : 

```mu(s) = beta0 + Sigma[ beta_j * (s1^n_j) * (s2^m_j) ]```

#### Geostatistical Kriging
    //Geostatistical kriging, tuples of (prediction, variance) per prediction point
    val attrFunc: (Double, Double) => Array[Double] = {
      (x, y) => Array(x, y, x * x, x * y, y * y)
    }
    
    val krigingVal: Array[(Double, Double)] =
        new GeoKriging(points, attrFunc, 50, Spherical)
          .predict(location)
    /**
      * The user can also do Geostatistical Kriging using :
      * new GeoKriging(points).predict(location)
      * new GeoKriging(points, bandwidth).predict(location)
      * new GeoKriging(points, model).predict(location)
      * new GeoKriging(points, bandwidth, model).predict(location)
      * new GeoKriging(points, attrFunc).predict(location)
      * new GeoKriging(points, attrFunc, bandwidth).predict(location)
      * new GeoKriging(points, attrFunc, model).predict(location)
      * new GeoKriging(points, attrFunc, bandwidth, model).predict(location)
      */

It is belongs to the class of General Spatial Prediction Models.

This model relaxes the assumption that the covariance is known.
Thus, the beta values and covariances are simultaneously evaluated and is computationally more intensive.

    mu(s) = x(s)' * beta,   beta unknown; s belongs to R
    cov[eps(s), eps(s')]    unknown; s, s' belongs to R
