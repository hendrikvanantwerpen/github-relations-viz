set terminal pngcairo transparent enhanced font "arial,10" size 800, 600 
set output "link-degree-histogram.png"
set title "Link degree" 
set logscale x
set logscale y
set xrange [ 0.8 : ]
set xtic scale 0 font ",8"
plot 'link-degree-histogram.dat' using 1:2 with impulses title "", \
     'link-degree-histogram.dat' using 1:2 with points title ""
