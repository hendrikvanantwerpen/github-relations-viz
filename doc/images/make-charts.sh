#!/bin/bash
for i in *.gnuplot; do
  gnuplot "$i"
done
