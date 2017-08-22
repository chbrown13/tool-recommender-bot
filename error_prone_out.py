import os
import re
import sys
import yaml


def _write(data, outfile):
	print data
	with open(outfile, 'w') as f:
		yaml.dump(data, f, default_flow_style=False)


def _parse_file(filename):
    with open(filename, 'r') as f:
		text = f.read()
		return text


def parse(log, out):
	if os.path.isfile(log):
		log = _parse_file(log)
	errors = {}
	lines = log.split('\n')
	for line in lines:
		err_match = re.search(r".java:\d+:", line)
		if err_match:
			err = line.split(':')
			msg = err[-1]
			err_dict = {}
			err_dict['filepath'] = err[0]
			err_dict['filename'] = err[0].split('/')[-1]
			err_dict['line'] = err[1]
			err_dict['message'] = msg
			err_dict['name'] = msg[msg.index('[')+1:msg.index(']')] if '[' in msg else None
			errors[':'.join([err[0], err[1], err[2]])] = err_dict
	_write(errors, out)


def compare(base, changed):
	print 'compare'


def main():
	mode = sys.argv[1]
	if mode == 'parse':
		outfile = sys.argv[3] if len(sys.argv) > 2 else 'master.yaml'
		parse(sys.argv[2], outfile)
	else: # compare parsed files
		compare(sys.argv[2], sys.argv[3])


if __name__ == "__main__":
	main()
