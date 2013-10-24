set term png
set output "out/hypotheekrenteaftrek.png"
set yrange [-0.1:1.2]
plot "hypotheekrenteaftrek.dat" using 1:2 title "Judge 1" with lines,"hypotheekrenteaftrek.dat" using 1:3 title "Judge 2" with lines