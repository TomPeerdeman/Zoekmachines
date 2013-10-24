set term png
set output "out/kernwapen_beleid_iran.png"
set yrange [-0.1:1.2]
plot "kernwapen_beleid_iran.dat" using 1:2 title "Judge 1" with lines,"kernwapen_beleid_iran.dat" using 1:3 title "Judge 2" with lines