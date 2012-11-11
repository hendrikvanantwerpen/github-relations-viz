set terminal pngcairo transparent enhanced font "arial,10" size 800, 600 
set output "link-weight-histogram.png"
set title "Link weight histogram" 

set logscale y
set ylabel "Number of links"

set logscale x
set xrange [ 0.8 : ]
set xtics scale 0 font ",8"
set xlabel "Link weight"

plot 'link-weight-histogram.dat' using 1:2 with impulses title "", \
     'link-weight-histogram.dat' using 1:2 with points title ""
