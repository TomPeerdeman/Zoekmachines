set term png
set output "out/de_film_beyond_fitna.png"
set yrange [-0.1:1.2]
plot "de_film_beyond_fitna.dat" using 1:2 title "Judge 1" with lines,"de_film_beyond_fitna.dat" using 1:3 title "Judge 2" with lines