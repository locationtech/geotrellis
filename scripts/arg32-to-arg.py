#!/usr/bin/env python
from os.path import basename
import struct
import sys

paths = sys.argv[1:]

for path in paths:
    assert path.endswith('.arg32')

for path in paths:
    path2 = path[:-2]

    print 'converting %s -> %s' % (basename(path), basename(path2))
    f = open(path, 'rb')
    g = open(path2, 'wb')
    
    bufsize = 1024 * 1024
    
    arg32 = struct.Struct('i')
    arg = struct.Struct('b')
    
    while True:
        bytes = f.read(bufsize)
        if not bytes: break
        i = 0
        while i < len(bytes):
            b0 = bytes[i]
            b1 = bytes[i + 1]
            b2 = bytes[i + 2]
            b3 = bytes[i + 3]

            if b0 == '\0' and b1 == '\0' and b2 == '\0' and '\0' < b3 and b3 < 'e':
                g.write(b3)
            else:
                g.write('\0')
            i += 4
    
    f.close()
    g.close()
