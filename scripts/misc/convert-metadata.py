#!/usr/bin/python

from lxml import etree
import os
import sys

format = """{
  "layer": "%(layer)s",
  "xmin": %(xmin)s,
  "xmax": %(xmax)s,
  "ymin": %(ymin)s,
  "ymax": %(ymax)s,
  "rows": %(rows)s,
  "cols": %(cols)s,
  "cellwidth": %(cellwidth)s,
  "cellheight": %(cellheight)s
}
"""

def grab(tree, xpath, default=None):
    results = tree.xpath(xpath)
    if results:
        return results[0]
    elif default is None:
        raise Exception("couldn't find %r" % xpath)
    else:
        return default

if __name__ == "__main__":
    for path in sys.argv[1:]:
        name, ext = os.path.splitext(path)
        tree = etree.parse(path)

        d = {
            'layer': name,
            'cellwidth': float(grab(tree, '/METADATA/CELLWIDTH/@value')),
            'cellheight': float(grab(tree, '/METADATA/CELLHEIGHT/@value')),
            'cols': int(grab(tree, '/METADATA/DIMENSIONS/@width')),
            'rows': int(grab(tree, '/METADATA/DIMENSIONS/@height')),
            'xmin': float(grab(tree, '/METADATA/ORIGIN/@xMin')),
            'ymin': float(grab(tree, '/METADATA/ORIGIN/@yMin')),
            'type': 'arg'
        }

        d['xmax'] = d['xmin'] + (d['cellwidth'] * d['cols'])
        d['ymax'] = d['ymin'] + (d['cellheight'] * d['rows'])

        json = format % d

        jpath = name + '.json'
        f = open(jpath, 'w')
        f.write(json)
        f.close()
