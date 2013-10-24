import sys
import re
import os

try:
	os.makedirs("plot/out")
except Exception:
    pass

j1 = {}
j2 = {}

f1 = open(sys.argv[1], 'r')
f2 = open(sys.argv[2], 'r')

regex = re.compile('[1|0](:?, )?')

subject = ''
for line in f1:
	if re.match(regex, line):
		l = line.split(', ')
		atp = []
		relarray = []
		i = 0
		rel = 0.0
		for item in l:
			i += 1
			if int(item) == 1:
				rel += 1.0
				relarray.append(1)
			else:
				relarray.append(0)
			atp.append(rel / i)

		j1[subject] = (atp, relarray)
	else:
		subject = line.rstrip()

subject = ''
for line in f2:
	if re.match(regex, line):
		l = line.split(', ')
		atp = []
		relarray = []
		i = 0
		rel = 0.0
		for item in l:
			i += 1
			if int(item) == 1:
				rel += 1.0
				relarray.append(1)
			else:
				relarray.append(0)
			atp.append(rel / i)

		j2[subject] = (atp, relarray)
	else:
		subject = line.rstrip()

f1.close()
f2.close()
			
ap_sum = 0.0 
for query in j1:
	space_safe = query.replace(' ', '_')
	f = open('plot/' + space_safe + '.dat', 'w')
	atp1, rel1 = j1[query]
	atp2, rel2 = j2[query]
	rel = 0.0
	summ = 0.0
	for i in range(0, len(atp1)):
		f.write("%d\t%f\t%f\n" % (i, atp1[i], atp2[i]))
		if rel1[i] == 1 and rel2[i] == 1:
			rel += 1.0
			summ += rel / (i + 1)

	print "AP(" + query + ")"
	print summ / rel
	ap_sum += (summ / rel)

	f.close()

	f = open('plot/' + space_safe + '.gnuplot', 'w')
	f.write('set term png\n')
	f.write('set output "out/' + space_safe + '.png"\n')
	f.write('set yrange [-0.1:1.2]\n')
	f.write('plot "' + space_safe + '.dat" using 1:2 title "Judge 1" with lines,')
	f.write('"' + space_safe + '.dat" using 1:3 title "Judge 2" with lines')
	f.close()

print "\nMAP"
print ap_sum / len(j1)

os.chdir("plot")
os.system("gnuplot *.gnuplot")
	
