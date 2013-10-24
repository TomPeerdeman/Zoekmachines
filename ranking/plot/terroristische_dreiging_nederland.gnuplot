set term png
set output "out/terroristische_dreiging_nederland.png"
set yrange [-0.1:1.2]
plot "terroristische_dreiging_nederland.dat" using 1:2 title "Judge 1" with lines,"terroristische_dreiging_nederland.dat" using 1:3 title "Judge 2" with lines