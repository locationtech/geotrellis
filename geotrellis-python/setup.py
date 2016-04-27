try:
        from setuptools import setup
except ImportError:
        from distutils.core import setup

setup(
        name='Geotrellis',
        version='0.1',
        description='Geographic data processing engine',
        author='Geotrellis team',
        url='https://github.com/geotrellis/geotrellis',
        install_requires = ['avro >=1.8.0'],
        packages=['geotrellis']
        )
