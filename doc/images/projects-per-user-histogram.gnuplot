set terminal pngcairo transparent enhanced font "arial,10" size 800, 600 
set output "projects-per-user-histogram.png"
set title "Projects per user" 

set logscale y
set ylabel "Number of users"

set logscale x
set xrange [ 0.8 : ]
set xtics scale 0 font ",8"
set xlabel "Number of projects involved in"

plot 'projects-per-user-histogram.dat' using 1:2 with impulses title "", \
     'projects-per-user-histogram.dat' using 1:2 with points title ""
