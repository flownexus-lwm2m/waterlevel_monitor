#!/usr/bin/env python3

import subprocess
import os
from pathlib import Path

CI_PATH = str(Path(__file__).resolve().parent)
CI_SCRIPT = os.path.join(CI_PATH, 'check_compliance.py')

# Compliance
# Print without newline
print("Running Compliance Check... ", end='', flush=True)
p = subprocess.Popen((CI_SCRIPT,
                      '-m', 'checkpatch',
                      '-m', 'Gitlint',
                      '-m', 'Devicetree',
                      '-m', 'Identity',
                      '-m', 'Nits',
                      '-m', 'pylint',
                      '-c', 'origin/main..HEAD'),
                      cwd = CI_PATH,
                      stdout=subprocess.PIPE,
                      stderr=subprocess.PIPE)

out, err = p.communicate()
out = out.decode('utf-8')
err = err.decode('utf-8')

if p.returncode == 0:
    print("[OK]")
else:
    print("[FAIL]")
    print(err)


def run_twister(test_directory):
    print(f"Building configurations in \"{test_directory}\": ", end='', flush=True)

    p = subprocess.Popen(('west', 'twister',
                          '-T', "../" + test_directory,
                          '-v',
                          '--inline-logs',
                          '--integration'),
                         cwd=CI_PATH,
                         stdout=subprocess.PIPE,
                         stderr=subprocess.PIPE)

    out, err = p.communicate()
    out = out.decode('utf-8')
    err = err.decode('utf-8')

    if p.returncode == 0:
        print("[OK]")
    else:
        print("[FAIL]")
        print(err)

run_twister("fw_test")
