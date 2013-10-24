set term png
set output "out/referendum_europese_grondwet.png"
set yrange [-0.1:1.2]
plot "referendum_europese_grondwet.dat" using 1:2 title "Judge 1" with lines,"referendum_europese_grondwet.dat" using 1:3 title "Judge 2" with lines