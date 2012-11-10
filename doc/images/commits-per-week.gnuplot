set terminal pngcairo transparent enhanced font "arial,10" size 800, 600 
set output "commits-per-week.png"
set title "Commits by date" 
set logscale y
set timefmt "%y-%m-%d"
set xdata time
set xtic rotate by -45 scale 0 font ",8"
plot 'commits-per-week.dat' using 1:2 with impulses title "", \
     'commits-per-week.dat' using 1:2 with points title ""
