# RDFPatch Stream

Experimental byte-level patch application based on sorted canonical n-quads and rdfpatch files.

Status: Working but just single threaded and comparatively slow:
`pv` reports a throughput of ~20MB/s with SSD and 12th Gen Intel(R) Core(TM) i9-12900HK.

* 💡 For patching sorted N-Quad files use [Scaseco/rdfpatch-nq-posix](https://github.com/Scaseco/rdfpatch-nq-posix) which is ~20x faster (~400MB/s).

Invocation with native Java compression decoding (via commons-compress)

```bash
rdfpatch-stream apply \
  wikidata-20250723-truthy-BETA.sorted.nt.bz2
  wikidata-20250723-to-20250918-truthy-BETA.sorted.rdfp.bz2
```

Compression decoding via process substitution:

```bash
rdfpatch-stream apply \
  <(lbzcat wikidata-20250723-truthy-BETA.sorted.nt.bz2)
  <(lbzcat wikidata-20250723-to-20250918-truthy-BETA.sorted.rdfp.bz2)
```


## Patch creation

Sorting wikidata truthy (40GB bz2 compressed, ~1TB plain nq) takes ~2 hours or less on sufficiently modern hardware.

Step 1: Sort nquads using `sort -u`. Adjust memory, decoding and re-encoding as needed.

Example:
```bash
lbzcat "INPUT.nq.bz2" | LC_ALL=C sort -u -S 80g | lbzip2 -cz > "OUTPUT.nq.bz2"
```

Step 2: Use `comm` and `awk` to create the patch (very fast). 
Adapt encoding and re-encoding to your needs.

Example:
```bash
#!/bin/bash
# create-patch.sh
# This creates a single file from sorted input with added / deleted lines prefixed by A/D, respectively.

OLD="$1"
NEW="$2"

if [ -z "$OLD" -o -z "$NEW" ]; then
  echo "Usage: old.nq.bz2 new.nq.bz2"
  exit 1
fi

# Lines start with A (added) and D (deleted) according to:
# https://afs.github.io/rdf-delta/rdf-patch.html

LC_ALL=C comm -3 <(lbzcat "$OLD") <(lbzcat "$NEW") | awk '
  /^[^\t]/     { print "D " $0; next }   # No leading tab -> only in file1 (removed)
  /^\t[^\t]/   { sub(/^\t/, "", $0); print "A " $0 }  # One leading tab -> only in file2 (added)
'
```

