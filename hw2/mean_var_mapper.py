#!/usr/bin/env python
import sys

chunk_sum = 0.0
chunk_square = 0.0
chunk_count = 0
for line in sys.stdin:
    line = line.strip()
    try:
        price = int(line.split(',')[-7])
    except IndexError:
        continue
    except ValueError:
        continue
    chunk_sum += price
    chunk_square += price * price
    chunk_count += 1

chunk_mean = chunk_sum / chunk_count
chunk_var = chunk_square / chunk_count - chunk_mean * chunk_mean

print '1\t%d\t%15.8f\t%15.8f' % (chunk_count, chunk_mean, chunk_var)