set term png
set output "out/pim_fortuyn.png"
set yrange [-0.1:1.2]
plot "pim_fortuyn.dat" using 1:2 title "Judge 1" with lines,"pim_fortuyn.dat" using 1:3 title "Judge 2" with lines