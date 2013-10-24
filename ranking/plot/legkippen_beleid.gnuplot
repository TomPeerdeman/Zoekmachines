set term png
set output "out/legkippen_beleid.png"
set yrange [-0.1:1.2]
plot "legkippen_beleid.dat" using 1:2 title "Judge 1" with lines,"legkippen_beleid.dat" using 1:3 title "Judge 2" with lines