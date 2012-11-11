set terminal pngcairo transparent enhanced font "arial,10" size 800, 600 
set output "user-project-links-per-week.png"
set title "User-project links per week" 

set logscale y
set ylabel "Number of user-project links"

set timefmt "%s"
set xdata time
set xtics 52*7*24*3600 rotate by -45 scale 0 font ",8" format "%Y"
set xlabel "Date"

plot 'user-project-links-per-week.dat' using 1:2 with impulses title "", \
     'user-project-links-per-week.dat' using 1:2 with points title ""
