try:
        from setuptools import setup, find_packages
except ImportError:
        from distutils.core import setup, find_packages

setup(
        name='Geotrellis',
        version='0.1',
        description='Geographic data processing engine',
        author='Geotrellis team',
        url='https://github.com/geotrellis/geotrellis',
        install_requires = ['avro >=1.8.0'],
        packages=find_packages(),
        include_package_data=True
        )
