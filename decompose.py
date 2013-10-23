import os
import re

outputFile = open("SQLinsert.txt", "w")

os.chdir("KVR")

for files in os.listdir("."):
    if files.endswith(".xml"):
        text = open(files).read()
        insertString = "INSERT INTO tabel (DocumentId, QuestionDate, AnswerDate, Questioner, QuestionerParty, Answerer, Keywords, Vragen, Antwoorden) VALUES ("
        
        try:
            DocumentId = "'" + re.search('<item attribuut="Document-id">(.+?)</item>', text).group(1) + "',"
        except AttributeError:
            DocumentId = '' 

        try:
            QuestionDate = "'" + re.search('<item attribuut="Datum_indiening">(.+?)</item>', text).group(1) + "',"
        except AttributeError:
            QuestionDate = ''

        try:
            AnswerDate = "'" + re.search('<item attribuut="Datum_reaktie">(.+?)</item>', text).group(1) + "',"
        except AttributeError:
            AnswerDate = '' 

        #TODO: allow multiple questioners
        try:
            Questioner = "'" + re.search('">(.+?)</persoon>', text).group(1) + "',"
        except AttributeError:
            Questioner = '' 

        try:
            QuestionerParty = "'" + re.search('<persoon partij="(.+?)">', text).group(1) + "',"
        except AttributeError:
            QuestionerParty = '' 

        #TODO: add repliers party
        try:
            Answerer = "'" + re.search('">(.+?)</antwoorder>', text).group(1) + "',"
        except AttributeError:
            Answerer = '' 

        try:
            Keywords = "'" + re.search('<item attribuut="Trefwoorden">(.+?)</item>', text).group(1) + "',"
        except AttributeError:
            Keywords = '' 

        #TODO: add multiple questions
        try:
            Vragen = "'" + re.search('>(.+?)</vraag>', text).group(1) + "',"
        except AttributeError:
            Vragen = '' 

        try:
            Antwoorden = "'" + re.search('">(.+?)</antwoord>', text).group(1) + "',"
        except AttributeError:
            Antwoorden = ''

        insertString += DocumentId
        insertString += QuestionDate
        insertString += AnswerDate
        insertString += Questioner
        insertString += QuestionerParty
        insertString += Answerer
        insertString += Keywords
        insertString += Vragen
        insertString += Antwoorden
        insertString += ");\n"
        insertString = insertString.replace("',);", "');")
        outputFile.write(insertString)
        
outputFile.close()
