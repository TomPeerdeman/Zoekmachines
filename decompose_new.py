import os
import sys
import xml.parsers.expat
import datetime

if len(sys.argv) != 2:
	exit(1)

item_mapping = {'Inhoud': 'contents', 'Rubriek': 'category', 'Afkomstig_van': 'origin', 'Document-id': 'doc_id'}

stack = []

insert = {}
vragen = []
text = ""

def startElement(name, attr):
	stack.append((name, attr))
	global text
	text = ""

def textElement(data):
	global text
	text += data

def endElement(elemName):
	name, attr = stack.pop()
	if elemName == name:
		if name == 'item':
			if attr['attribuut'] in item_mapping.keys():
				insert[item_mapping[attr['attribuut']]] = text
			elif attr['attribuut'] == 'Datum_indiening':
				date = datetime.datetime.strptime(text, '%Y-%m-%d').date()
				insert['entering_year'] = date.year;
				insert['entering_month'] = date.month;
				insert['entering_day'] = date.day;
			elif attr['attribuut'] == 'Datum_reaktie':
				date = datetime.datetime.strptime(text, '%Y-%m-%d').date()
				insert['answering_year'] = date.year;
				insert['answering_month'] = date.month;
				insert['answering_day'] = date.day;
		elif name == 'vraag':
			print ''			


for file_name in os.listdir(sys.argv[1]):
	p = xml.parsers.expat.ParserCreate()
	p.StartElementHandler = startElement
	p.EndElementHandler = endElement
	p.DefaultHandler = textElement
	p.ParseFile(open(sys.argv[1] + '/' + file_name, 'r'))
	
	if len(insert) > 0:
		columns = ""
		fields = ""
		for k, v in insert.iteritems():
			columns += "%s, " % k
			fields += "'%s', " % v

		# Cut last , off
		columns = columns[:-2]
		fields = fields[:-2]

		sql = "INSERT INTO documents (" + columns + ") VALUES (" + fields + ")"
		print sql

	# Only process one for debugging
	exit()

