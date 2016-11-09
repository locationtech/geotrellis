try:
    from setuptools import setup
except ImportError:
    from distutils.core import setup

    config = {
        'description': 'pygeotrellis',
        'author': 'Jacob Bouffard',
        'download_url': 'http://github.com/geotrellis/geotrellis',
        'author_email': 'jbouffard@azavea.com',
        'version': '0.1',
        'install_requires': ['py4j'],
        'packages': ['pygeotrellis'],
        'scripts': [],
        'name': 'geotrellis-python'
    }

    setup(**config)
