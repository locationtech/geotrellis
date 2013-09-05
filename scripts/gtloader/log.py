"""
Cheap and easy logging
probably should use python logging
"""

def error(m,code=1):
    print 'ERROR: %s' % m
    exit(code)

def warn(m):
    print 'WARNING: %s' % m

def notice(m):
    print 'NOTICE: %s' % m
