import os
import sys
import xml.parsers.expat
import datetime

if len(sys.argv) != 3:
	print "Usage python decompose_new.py {files directory} {output sql file}"
	exit(1)

outputFile = open(sys.argv[2], "w")

# XML <item attribute="x"> --> db column mapping
item_mapping = {'Inhoud': 'contents', 'Rubriek': 'category', 'Afkomstig_van': 'origin', 'Document-id': 'doc_id'}

# XML element stack
stack = []

# {column: value} dict of document insert
insert = {}
# List of tuples (nummer, vraag)
vragen = []
# List of tuples (nummer, antwoord)
antwoorden = []
# List of tuples (fuctie, ministerie, naam)
antwoorders = []
# List of tuples (partij, naam)
vragers = []

# General counters
ndocs = 0
nquestions = 0
nquestioners = 0
nanswers = 0
nanswerers = 0

text = ""

def startElement(name, attr):
	stack.append((name, attr))
	global text
	# voetnoot element is ignored
	if name != 'voetnoot':
		text = ""

def textElement(data):
	global text
	text += data

def endElement(elemName):
	name, attr = stack.pop()
	if elemName == name:
		# Parse <item elements
		if name == 'item':
			# Std item attribute, map name to db column name
			if attr['attribuut'] in item_mapping.keys():
				# strip() removes leading and trailing whitespaces
				insert[item_mapping[attr['attribuut']]] = text.strip()
			# Parse dates as yyyy-mm-dd values
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
		# Parse <vraag element
		elif name == 'vraag':
			vragen.append((attr['nummer'], text.strip()))	
		# Parse <antwoord element
		elif name == 'antwoord':
			antwoorden.append((attr['nummer'], text.strip()))		
		# Parse <antwoorder element
		elif name == 'antwoorder':
			# Todo: parse mede-antwoorder?
			antwoorders.append((attr['functie'], attr['ministerie'], text.strip()))	
		# Parse <vrager element
		elif name == 'vrager':
			vragers.append((attr['partij'], text.strip()))	


for file_name in os.listdir(sys.argv[1]):
	if file_name.endswith('.xml'):
		# New XML parser
		p = xml.parsers.expat.ParserCreate()
		p.StartElementHandler = startElement
		p.EndElementHandler = endElement
		p.DefaultHandler = textElement

		# Open the file and parse it
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

			# Insert document
			outputFile.write("INSERT INTO documents (" + columns.encode("UTF-8") + ") VALUES (" + fields.encode("UTF-8") + ");")
			ndocs += 1

			# Insert questioners 
			for party, name in vragers:
				outputFile.write("INSERT INTO questioners (doc_id, questioner_party, questioner_name) VALUES ((SELECT id FROM documents WHERE doc_id = '" + insert['doc_id'].encode("UTF-8") + "'), '" + party.encode("UTF-8") + "', '" + name.encode("UTF-8") + "');")
				nquestioners += 1

			# Insert questions
			for num, question in vragen:
				outputFile.write("INSERT INTO questions (doc_id, question_id, question_text) VALUES ((SELECT id FROM documents WHERE doc_id = '" + insert['doc_id'].encode("UTF-8") + "'), '" + num.encode("UTF-8") + "', '" + question.encode("UTF-8") + "');")
				nquestions += 1

			# Insert answerers
			for function, ministery, name in antwoorders:
				outputFile.write("INSERT INTO questions (doc_id, answerer_function, answerer_ministry, answerer_name) VALUES ((SELECT id FROM documents WHERE doc_id = '" + insert['doc_id'].encode("UTF-8") + "'), '" + function.encode("UTF-8") + "', '" + ministery.encode("UTF-8") + "', '" + name.encode("UTF-8") + "');")
				nanswerers += 1

			# Insert answers
			for num, answer in antwoorden:
				outputFile.write("INSERT INTO answers (doc_id, question_id, answer_text) VALUES ((SELECT id FROM documents WHERE doc_id = '" + insert['doc_id'].encode("UTF-8") + "'), '" + num.encode("UTF-8") + "', '" + answer.encode("UTF-8") + "')")
				nanswers += 1
		
		# Reset data after file is parsed
		stack = []
		text = ""
		insert = {}
		vragen = []
		antwoorden = []
		antwoorders = []
		vragers = []

outputFile.close()

# Print counters
print "# Documents %d" % ndocs
print "# Questioners: %d" % nquestioners
print "# Questions: %d" % nquestions
print "# Answerers: %d" % nanswerers
print "# Answers: %d" % nanswers

