import java_gateway
from pyspark import SparkConf, SparkContext

def main():
    gateway = java_gateway.launch_java_gateway()
    jvm = gateway.jvm

    path = "../econic.tif"

    conf = SparkConf()
    conf.setMaster("local")
    conf.setAppName("py-test")
    sc = SparkContext(conf = conf)

    singleband_geotiff = jvm.HadoopSpatialSinglebandRDD(path, sc)
    splits = singleband.split(25, 25)

    print len(splits)

if __name__ == "__main__":
    main()
