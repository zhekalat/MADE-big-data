#!/usr/bin/env python
import sys
import math

current_mean = 0
current_var = 0
current_count = 0
word = None

for line in sys.stdin:
    line = line.strip()
    key, ck, mk, vk = line.split('\t', 3)
    try:
        ck = int(ck)
        mk = float(mk)
        vk = float(vk)
    except:
        continue
    current_mean = (current_mean * current_count + mk * ck) / (current_count + ck)
    current_var = ((current_var * current_count + vk * ck) / (current_count + ck) +
                   current_count * ck * math.pow((current_mean - mk) / (current_count + ck), 2))
    current_count += ck

print '%d\t%15.8f\t%15.8f' % (chunk_count, chunk_mean, chunk_var)