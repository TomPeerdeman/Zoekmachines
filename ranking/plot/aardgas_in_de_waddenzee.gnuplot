set term png
set output "out/aardgas_in_de_waddenzee.png"
set yrange [-0.1:1.2]
plot "aardgas_in_de_waddenzee.dat" using 1:2 title "Judge 1" with lines,"aardgas_in_de_waddenzee.dat" using 1:3 title "Judge 2" with lines