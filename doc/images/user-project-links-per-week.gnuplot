set terminal pngcairo transparent enhanced font "arial,10" size 800, 600 
set output "user-project-links-per-week.png"
set title "User-projectlinks per week" 
set logscale y
set timefmt "%y-%m-%d"
set xdata time
set xtic rotate by -45 scale 0 font ",8"
plot 'user-project-links-per-week.dat' using 1:2 with impulses title "", \
     'user-project-links-per-week.dat' using 1:2 with points title ""
