#!/usr/bin/env python
import sys

f = open(sys.argv[1], 'rb')
g = open(sys.argv[1] + '32', 'wb')

while True:
    bytes = f.read(1024)
    if not bytes: break
    for b in bytes:
        if b == '\0':
            g.write('\200\0\0\0')
        else:
            g.write('\0\0\0' + b)

f.close()
g.close()
