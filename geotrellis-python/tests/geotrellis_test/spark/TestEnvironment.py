from __future__ import absolute_import
from nose import tools

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
from geotrellis.python.util.utils import dir_exists, fullname
import os
import os.path


def get_temp_dir():
    #return os.environ["java.io.tmpdir"] # TODO is it ok to use java.io.tmpdir here?
    return "/tmp"

class _TestEnvironment(object):
    _sc = None
    def __init__(self):
        self.outputHome = "testFiles"
        self.afterAlls = []
        self.sc
        self._jvm = self.sc._jvm
        self.name = fullname(type(self))
        self.conf = SparkUtils.hadoopConfiguration(self._jvm)
        self.localFS = _TestEnvironment.getLocalFS(self.conf, self._jvm)
        self.inputHome = _TestEnvironment.inputHome(self._jvm)
        self.inputHomeLocalPath = self.inputHome.toUri().getPath()
        def outputPaths(jvm):
            tmpdir = get_temp_dir()
            outputHomeLocalHandle = os.path.join(tmpdir, self.outputHome)
            if dir_exists(outputHomeLocalHandle):
                import shutil
                shutil.rmtree(outputHomeLocalHandle)
            if not dir_exists(outputHomeLocalHandle):
                os.makedirs(outputHomeLocalHandle)
            hadoopTmpDir = HdfsUtils.getTempDir(self.conf)

            outputLocalHandle = os.path.join(outputHomeLocalHandle, self.name)
            if not dir_exists(outputLocalHandle):
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

    def registerAfterAll(self, func):
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
        # TODO
        #conf.set("spark.kryo.registrator", "geotrellis.spark.TestRegistrator")
        pass

    @property
    def sc(self):
        if _TestEnvironment._sc:
            return _TestEnvironment._sc

        os.environ["spark.driver.port"] = "0"
        os.environ["spark.hostPort"] = "0"
        os.environ["spark.ui.enabled"] = "false"

        conf = SparkConf()
        conf.setMaster("local")
        conf.setAppName("Test Context")

        # <python-version-only>
        conf.set('spark.yarn.dist.files','file:{pysparkpath},file:{py4jpath}'.format(
            pysparkpath = os.path.join(os.environ['SPARK_HOME'], "python", "lib", "pyspark.zip"),
            py4jpath = os.path.join(os.environ['SPARK_HOME'], "python", "lib", "py4j-0.8.2.1-src")
            ))
        conf.setExecutorEnv('PYTHONPATH','pyspark.zip:py4j-0.8.2.1-src.zip')
        # </python-version-only>

        #if "GEOTRELLIS_USE_JAVA_SER" not in os.environ.keys():
        #    conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
        #    conf.set("spark.kryoserializer.buffer.max", "500m")
        #    self.setKryoRegistrator(conf)

        def eggFileLocation():
            currentpath = os.path.dirname(os.path.realpath(__file__))
            folder_name = 'geotrellis-python'
            ind = currentpath.rfind(folder_name) + len(folder_name)
            currentpath = currentpath[:ind]
            return os.path.join(currentpath, 'dist', 'Geotrellis-0.1-py2.7.egg')

        sparkContext = SparkContext(conf = conf, pyFiles = ['file:{egg}'.format(egg=eggFileLocation())])

        del os.environ["spark.driver.port"]
        del os.environ["spark.hostPort"]
        del os.environ["spark.ui.enabled"]
        
        _TestEnvironment._sc = sparkContext
        return sparkContext

    def mkdir(self, _dir):
        handle = dir.toUri().toString()
        if not dir_exists(handle):
            os.makedirs(handle)

    def clearTestDirectory(self):
        f = self._jvm.java.io.File(self.outputLocal.toUri().toString())
        self._jvm.org.apache.hadoop.fs.FileUtil.fullyDelete(f)

    def afterAll(self):
        self.clearTestDirectory()
        if _TestEnvironment._sc is not None:
            self.sc.stop()
            _TestEnvironment._sc = None
        if self.afterAlls:
            for func in self.afterAlls:
                func()

    def _getLocalFS(self):
        return self._jvm.org.apache.hadoop.fs.Path(get_temp_dir()).getFileSystem(self.conf)
