set term png
set output "out/massamoord_srebrenica.png"
set yrange [-0.1:1.2]
plot "massamoord_srebrenica.dat" using 1:2 title "Judge 1" with lines,"massamoord_srebrenica.dat" using 1:3 title "Judge 2" with lines