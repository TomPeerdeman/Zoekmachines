import os
import sys
import requests

if len(sys.argv) != 3:
	print "Usage python mass_upload {upload_url} {file directory}"
	exit(1)

success = 0
fail = 0

for file_name in os.listdir(sys.argv[2]):
	if file_name.endswith('.xml'):
		files = {'file': (file_name, open(sys.argv[2] + '/' + file_name, 'rb'))}

		sys.stdout.write("%s..." % file_name)
		r = requests.post(sys.argv[1], files=files)
		sys.stdout.write("\b\b\b: %d\n" % r.status_code)
		if r.status_code == 200:
			success += 1
		else:
			fail += 1

print("Success: %d" % success)
print("Failed: %d" % fail)

