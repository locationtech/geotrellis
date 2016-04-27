def zdiv(load_func, dims):
    def inner(xd, rmin, rmax):
        if rmin >= rmax:
            raise Exception("min ({0}) must be less than max ({1})".format(rmin, rmax))
        zmin = rmin
        zmax = rmax
        bigmin = 0L
        litmax = 0L

        def bit(x, idx):
            return (x & (1L << idx)) >> idx

        def over(bits):
            return 1L << (bits-1)

        def under(bits):
            return (1L << (bits-1)) - 1

        i = 64
        while i > 0:
            i -= 1
            bits = i/dims+1
            dim = i%dims

            a,b,c = bit(xd, i), bit(zmin, i), bit(zmax, i)
            if a == 0 and b == 0 and c == 0:
                # continue
                pass
            elif a == 0 and b == 0 and c == 1:
                zmax    = load_func(zmax, under(bits), bits, dim)
                bigmin  = load_func(zmin, over(bits), bits, dim)
            elif a == 0 and b == 1 and c == 0:
                #  sys.error(s"Not possible, MIN <= MAX, (0, 1, 0)  at index $i")
                pass
            elif a == 0 and b == 1 and c == 1:
                bigmin = zmin
                return (litmax, bigmin)
            elif a == 1 and b == 0 and c == 0:
                litmax = zmax
                return (litmax, bigmin)
            elif a == 1 and b == 0 and c == 1:
                litmax  = load(zmax, under(bits), bits, dim)
                zmin    = load(zmin, over(bits), bits, dim)
            elif a == 1 and b == 1 and c == 0:
                #  sys.error(s"Not possible, MIN <= MAX, (1, 1, 0) at index $i")
                pass
            elif a == 1 and b == 1 and c == 1:
                # continue
                pass
        return (litmax, bigmin)

    return inner

