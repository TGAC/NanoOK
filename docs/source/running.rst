.. _running:

Running NanoOK
==============

**Note about multi-read FAST5, FASTA, FASTQ:** Currently, multi-read FAST5 files cannot be processed by nanook extract. Multi-read FASTA/Q files cannot be processed natively by NanoOK yet, but you can use the multi_to_chunk_dirs.pl script to convert a multi-read FASTA/Q file into single read files that NanoOK align and analyse can process. See the `Multi-read files`_ section at the end of the page.

Overview
--------

NanoOK understands the concept of sample directories/folders. Within a
sample directory will be a set of subdirectories:

-  for FAST5 files (though these can exist outside of the subdirectory
   if you want)
-  for FASTA/Q files
-  for alignments, named after the aligner - e.g. "bwa" or "last"
-  for logs
-  for graphs 
-  for analysis
-  for the LaTeX report generated

With the exception of the FAST5 subdirectory, NanoOK will create
directories within the sample directory as it needs them.

NanoOK expects FAST5 files to be arranged in one of the following
standard ways:

+--------------------------------------+--------------------------------------+
| Metrichor structure                  | Albacore structure                   |
+======================================+======================================+
| -  N79681\_1stLambda\_8kb (the       | -  N79681\_1stLambda\_8kb (the       |
|    sample directory)                 |    sample directory)                 |
|                                      |                                      |
|    -  downloads                      |    -  workspace                      |
|                                      |                                      |
|       -  pass                        |       -  barcodeXXXX (if barcoded)   |
|                                      |                                      |
|          -  BCXX or barcodeXXX (if   |          -  0                        |
|             barcoded)                |                                      |
|                                      |             -  xxx.fast5             |
|             -  batch\_XXXX           |                                      |
|                                      |                                      |
|                -  xxxx.fast5         |                                      |
|                                      |                                      |
|       -  fail                        |                                      |
|                                      |                                      |
|          -  unaligned (if barcoded)  |                                      |
|                                      |                                      |
|             -  batch\_XXXX           |                                      |
|                                      |                                      |
|                -  xxx.fast5          |                                      |
+--------------------------------------+--------------------------------------+

You can use the -f parameter of nanook extract to tell the program where
your fast5 files are, e.g. -f workspace (for a relative path within the
sample directory) or -f /path/to/workspace (for an absolute path).

If your files are not like this, then NanoOK will probably struggle to
process them.

The next few sections assume the use of the LAST aligner - our currently
preferred choice. Later sections detail changes for other aligners.

For the impatient
-----------------
::

  lastdb -Q 0 referencename referencename.fasta
  nanook extract -s SampleDir -f path/to/fast5
  nanook align -s SampleDir -r referencename.fasta
  nanook analyse -s SampleDir -r referencename.fasta -passonly

Extracting FASTA files
----------------------

Use the nanook extract option::

  nanook extract -s SampleDir

where:

-  ``-s`` or ``-sample`` specifies the sample name (ie. same as
   directory name)
-  ``-a`` or ``-fasta`` (optional, set by default) specifies FASTA
   output
-  ``-q`` or ``-fastq`` specifies FASTQ output
-  ``-f`` or ``-reads`` allows you to specify the location of the FAST5 reads.
   This needs to be either absolute (beginning with a '/' - e.g.
   ``-f /Users/leggett/examplerun/workspace``) or relative to the sample
   directory (e.g. ``-f workspace`` if the sample directory contains the
   albacore workspace directory).
-  ``-minquality`` allows you to set the minimum quality for a pass read.
   Without specifying this, your reads will be treated as per the
   basecaller's criteria. If the basecaller doesn't separate into
   pass/fail, all reads are considered pass.

Preparing references
--------------------

NanoOK supports multiple reference sequences - however, these currently
need to be located in a single FASTA file.

References first need to be indexed with the aligner, e.g. with LAST::

  lastdb -Q 0 referencename referencename.fasta

The -Q 0 tells LAST to expect a FASTA file, the next parameter is the
output prefix and the final parameter is the reference file.

**Note: the output prefix must be the same name as the FASTA file, apart
from the .fa or .fasta extension. This is because NanoOK needs to use
the original FASTA file.**

NanoOK also produces its own index file. It will do this when you first
run alignments and will generate a file called
``referencename.fasta.sizes``, which will look similar to::

  gi|556503834|ref|NC_000913.3|	4641652	Escherichia_coli

This is a three column format - the first column gives sequence IDs in
the FASTA file, the second the size of the sequence and the third the
display name used by NanoOK. The display name is also used for filenames
for various intermediate files.

NanoOK will extract meaningful display names from NCBI format sequences,
but it cannot cope with every format of sequence ID.  It is recommended
that every time you generate a new .sizes file, you check that you are
happy with the display names and that they do not contain characters
that can cause problems with filenames and LaTeX - e.g. \| (bar). You
can do this before running nanook analyse (below).

**If you make changes to the reference file, you will need to delete the
current .sizes file and re-generate it, otherwise there will be contigs
missing from it.**

Running alignments
------------------

To run alignments for all reads, type::

  nanook align -s SampleDir -r referencename.fasta

where:

-  ``-s``  or ``-sample`` specifies the sample name (ie. same as
   directory name)
-  ``-r`` or ``-reference`` specifies the name of the reference file to
   use (including the .fasta or .fa extension)
-  ``-aligner`` (optional) specifies the name of the aligner, which
   defaults to 'last'. Valid options are:

   -  ``last`` - for LAST
   -  ``bwa`` - for BWA-MEM
   -  ``blasr`` - for BLASR
   -  ``marginalign`` - for MarginAlign
   -  ``graphmap`` - for GraphMap

Running NanoOK analysis
-----------------------

NanoOK can be run from the command line as follows::

  nanook analyse -s SampleDir -r referencename.fasta -passonly

where:

-  ``-s`` or ``-sample`` specifies the sample name (ie. same as
   directory name).
-  ``-r`` or ``-reference`` specifies the name of the reference file to
   use.
-  ``-passonly`` tells NanoOK only to process the 'pass' directory. You
   can leave this out to analyse both pass and fail, or even specify a
   -failonly parameter if you just want to analyse the 'fail' reads .
-  ``-aligner`` specifies the aligner (default 'last'). Valid options
   are the same as for ``nanook align``.
-  ``-2donly`` will generate a report that contains only 2D data.
-  ``-bitmaps`` will generate PNG format graphs instead of the default
   PDF format. This can result in faster rendering of PDFs for reports
   with lots of reads.

This will generate a LaTeX file (with a .tex extension) and a
corresponding PDF within a latex subdirectory of the run directory. The
naming of the latex subdirectory depends on the aligner and options used
- e.g. latex\_last\_passonly for passonly alignments with LAST. This
naming is designed so that you can generate multiple reports with
different alignment tools or options.

Comparison reports
------------------

NanoOK comparison let you compare NanoOK analyses for multiple runs.
This enables comparison of, for example, chemistry versions, software
versions, alignment tools. The comparison option can be run as follows::

  nanook compare –l samples.txt –o outdir –type 2D

where:

-  ``-l``  or  ``-samplelist``  specifies a list of samples to compare
   (see below for format).
-  ``-o`` or ``-outputdir`` specifies an output directory to write
   analyses, graphs and report to.
-  ``-type`` specifies the type of data to compare - either 2D, Template
   or Complement.

This will generate a LaTeX file and a PDF file within a latex
subdirectory of the output directory. The sample list file is a two
column tab-separated file as follows::

  SampleDir	SampleName	dirname1	sample_1	dirname2	sample_2

The SampleDIr column is the same name you would specify to the -s
parameter of extract/align/analyse. The SampleName column is the display
name that will be used in graphs.

Multi-threading
---------------

You can control the maximum number of threads used by nanook by
specifying the ``-t`` or ``-numthreads`` parameter.

Barcoding
---------

As of NanoOK 1.15, barcoding directory structures should be
auto-detected.

1D data
-------

To avoid creating 2D and Complement directories when running with 1D
data, specify the ``-templateonly`` option.

Using BWA-MEM for alignments
----------------------------

You will need to index your reference with BWA::

  bwa index referencename.fasta

When running ``nanook align`` and ``nanook analyse``, make sure you
specify the ``-aligner bwa`` option.

Using BLASR for alignments
--------------------------

You do not need to index your reference separately with BLASR. 

When running ``nanook align`` and ``nanook analyse``, make sure you
specify the ``-aligner blasr`` option.

Using marginAlign for alignments
--------------------------------

marginAlign works from FASTQ files, so you will need to extract these
with the ``-q`` flag to ``nanook extract``::

  nanook extract -s <sample> -q

References do not need to be indexed with marginAlign.

When running ``nanook align`` and ``nanook analyse``, make sure you
specify the ``-a marginalign`` option.

Changing default aligner parameters
-----------------------------------

You can use the -alignerparams option to change the default tuning
parameters used for the aligners. To use, you must enclose the
parameters in speech marks, for example::

  nanook align -s SampleDir -r referencename.fasta -alignerparams "-s 2 -T 0 -Q 0 -a 1"

The table below shows the default parameters used by NanoOK for the
supported aligners:

+--------------------------------------+--------------------------------------+
| Aligner                              | Parameters                           |
+======================================+======================================+
| LAST                                 | "-s 2 -T 0 -Q 0 -a 1"                |
+--------------------------------------+--------------------------------------+
| BWA MEM                              | "-x ont2d"                           |
+--------------------------------------+--------------------------------------+
| BLASR                                | ""                                   |
+--------------------------------------+--------------------------------------+
| marginAlign                          | ""                                   |
+--------------------------------------+--------------------------------------+
| GraphMap                             | ""                                   |
+--------------------------------------+--------------------------------------+

Multi-read files
----------------

If you don’t have individual read files, but they are merged into a single
FASTA/Q file, NanoOK currently cannot process them. However, as a temporary fix,
we provide a script called ``multi_to_chunk_dirs.pl`` for you to split multiple FASTA/Q  files, for example::

  multi_to_chunk_dirs.pl -in input.fasta -out sampledir/fasta/pass/Template

 
