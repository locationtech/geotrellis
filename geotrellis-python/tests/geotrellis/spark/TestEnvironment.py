from nose import tools

@tools.nottest
def add_pyspark_path():
    """
    Add PySpark to the PYTHONPATH
    Thanks go to this project: https://github.com/holdenk/sparklingpandas
    """
    import sys
    import os
    import os.path
    try:
        sys.path.append(os.path.join(os.environ['SPARK_HOME'], "python"))
        sys.path.append(os.path.join(os.environ['SPARK_HOME'],
            "python","lib","py4j-0.8.2.1-src.zip"))
    except KeyError:
        print "SPARK_HOME not set"
        sys.exit(1)

add_pyspark_path() # Now we can import pyspark
from pyspark import SparkContext, SparkConf
from geotrellis.spark.util.SparkUtils import SparkUtils
from geotrellis.spark.io.hadoop.HdfsUtils import HdfsUtils
from geotrellis.python.util.utils import file_exists, fullname
import os
import os.path


@tools.nottest
def get_temp_dir():
    #return os.environ["java.io.tmpdir"] # TODO is it ok to use java.io.tmpdir here?
    return "/tmp"

@tools.nottest
class _TestEnvironment(object):
    def __init__(self):
        self.outputHome = "testFiles"
        self.afterAlls = []
        self.sc = None
        self._sc()
        self._jvm = self.sc._jvm
        self.name = fullname(type(self))
        self.conf = SparkUtils.hadoopConfiguration(self._jvm)
        self.localFS = _TestEnvironment.getLocalFS(self.conf, self._jvm)
        self.inputHome = _TestEnvironment.inputHome(self._jvm)
        self.inputHomeLocalPath = self.inputHome.toUri().getPath()
        def outputPaths(jvm):
            tmpdir = get_temp_dir()
            outputHomeLocalHandle = os.path.join(tmpdir, self.outputHome)
            if not file_exists(outputHomeLocalHandle):
                os.makedirs(outputHomeLocalHandle)
            hadoopTmpDir = HdfsUtils.getTempDir(self.conf)

            outputLocalHandle = os.path.join(outputHomeLocalHandle, self.name)
            if not file_exists(outputLocalHandle):
                os.makedirs(outputLocalHandle)
            return (jvm.org.apache.hadoop.fs.Path(outputHomeLocalHandle),
                    jvm.org.apache.hadoop.fs.Path(hadoopTmpDir),
                    jvm.org.apache.hadoop.fs.Path(outputLocalHandle),
                    outputLocalHandle)
        homeLocal, homeHdfs, local, localPath = outputPaths(self._jvm)
        self.outputHomeLocal = homeLocal
        self.outputHomeHdfs = homeHdfs
        self.outputLocal = local
        self.outputLocalPath = localPath

    def registerAfterAll(func):
        self.afterAlls.append(func)

    @staticmethod
    def getLocalFS(conf, jvm):
        tmpdir = get_temp_dir()
        return jvm.org.apache.hadoop.fs.Path(tmpdir).getFileSystem(conf)

    @staticmethod
    def inputHome(jvm):
        conf = SparkUtils.hadoopConfiguration(jvm)
        localFS = _TestEnvironment.getLocalFS(conf, jvm)
        return jvm.org.apache.hadoop.fs.Path(localFS.getWorkingDirectory(), "spark/src/test/recources/")

    def setKryoRegistrator(self, conf):
        conf.set("spark.kryo.registrator", "geotrellis.spark.TestRegistrator")

    def _sc(self):
        if self.sc:
            return self.sc

        os.environ["spark.driver.port"] = "0"
        os.environ["spark.hostPort"] = "0"
        os.environ["spark.ui.enabled"] = "false"

        conf = SparkConf()
        conf.setMaster("local")
        conf.setAppName("Test Context")

        if "GEOTRELLIS_USE_JAVA_SER" not in os.environ.keys():
            conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
            conf.set("spark.kryoserializer.buffer.max", "500m")
            self.setKryoRegistrator(conf)

        sparkContext = SparkContext(conf = conf)

        del os.environ["spark.driver.port"]
        del os.environ["spark.hostPort"]
        del os.environ["spark.ui.enabled"]
        
        self.sc = sparkContext
        return sparkContext

    def mkdir(self, _dir):
        handle = dir.toUri().toString()
        if not file_exists(handle):
            os.makedirs(handle)

    def clearTestDirectory(self):
        f = self._jvm.java.io.File(self.outputLocal.toUri())
        self._jvm.org.apache.hadoop.fs.FileUtil.fullyDelete(f)

    def afterAll(self):
        self.clearTestDirectory()
        sc.stop()
        if self.afterAlls:
            for func in self.afterAlls:
                func()

    def _getLocalFS(self):
        return self._jvm.org.apache.hadoop.fs.Path(get_temp_dir()).getFileSystem(self.conf)
