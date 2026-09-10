"""Temporarily remove room cleanup, capture a real failing test, and always restore the source.

Run from the repository root with Python 3 and JAVA_HOME pointing to JDK 17.
No database or Node installation is needed for this focused test.
"""
import difflib
import os
from pathlib import Path
import subprocess

root = Path(__file__).resolve().parents[1]
source = root / 'src/main/java/com/lumina/clinic/booking/BookingPolicy.java'
evidence = root / 'docs/test-evidence'
evidence.mkdir(parents=True, exist_ok=True)
original = source.read_bytes()
text = original.decode('utf-8')
rule = 'public static final int ROOM_TURNAROUND_MINUTES = 15;'
if text.count(rule) != 1:
    raise SystemExit('Expected cleanup rule not found exactly once; no files changed.')
mutated = text.replace(rule, 'public static final int ROOM_TURNAROUND_MINUTES = 0;')
relative = source.relative_to(root).as_posix()
diff = ''.join(difflib.unified_diff(text.splitlines(True), mutated.splitlines(True),
                                 fromfile='a/' + relative, tofile='b/' + relative))
(evidence / 'mutation.diff').write_text(diff, encoding='utf-8')
wrapper = str(root / ('mvnw.cmd' if os.name == 'nt' else 'mvnw'))
command = [wrapper, '-B', '-ntp', '-Dfrontend.skip=true', '-Dfrontend.tests.skip=true',
           '-Dtest=BookingPolicyTest#businessHoursIncludeRoomCleanupEvenForSeniorTherapists', 'test']
environment = {**os.environ, 'DEBUG': 'false'}

def run(name):
    result = subprocess.run(command, cwd=root, env=environment, capture_output=True, text=True,
                            encoding='utf-8', errors='replace', timeout=240)
    (evidence / name).write_text(result.stdout + result.stderr, encoding='utf-8')
    return result

try:
    source.write_text(mutated, encoding='utf-8', newline='')
    broken = run('mutation-failure.txt')
finally:
    source.write_bytes(original)

if broken.returncode == 0 or 'Failures: 1' not in broken.stdout:
    raise SystemExit('The changed rule did not produce the expected assertion failure; inspect the log.')
restored = run('rule-restored.txt')
if restored.returncode != 0:
    raise SystemExit('Source restored, but the passing verification failed; inspect rule-restored.txt.')
print('Verified: cleanup removed -> 1 assertion failure; source restored -> test passes.')
