#!/bin/bash
set -e

echo "Test 1: Single patch file"
java -cp target/classes com.example.sortedmerge.Main test/base.nt test/patch1.nt > test/output1.txt
cat > test/expected1.txt << 'EXPECTED'
<http://example.org/s0> <http://example.org/p0> "o0" .
<http://example.org/s2> <http://example.org/p2> "o2-modified" .
<http://example.org/s4> <http://example.org/p4> "o4" .
<http://example.org/s5> <http://example.org/p5> "o5" .
<http://example.org/s6> <http://example.org/p6> "o6" .
EXPECTED
diff test/output1.txt test/expected1.txt && echo "PASS" || echo "FAIL"

echo "Test 2: Multiple patch files"
java -cp target/classes com.example.sortedmerge.Main test/base.nt test/patch1.nt test/patch2.nt > test/output2.txt
cat > test/expected2.txt << 'EXPECTED'
<http://example.org/s0> <http://example.org/p0> "o0" .
<http://example.org/s10> <http://example.org/p10> "o10" .
<http://example.org/s2> <http://example.org/p2> "o2-modified" .
<http://example.org/s5> <http://example.org/p5> "o5" .
<http://example.org/s6> <http://example.org/p6> "o6" .
EXPECTED
diff test/output2.txt test/expected2.txt && echo "PASS" || echo "FAIL"

echo "Test 3: All deletions"
java -cp target/classes com.example.sortedmerge.Main test/base.nt test/delete_all.nt > test/output3.txt
[ ! -s test/output3.txt ] && echo "PASS" || echo "FAIL"

echo "All tests completed!"
