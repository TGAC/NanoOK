.. _nanookrt:

NanoOK RT
=========

Introduction
------------

NanoOK RT functionality is included within the core NanoOK software.
However, NanoOK repoRTer is available as a separate tool.

The RT functionality should be considered alpha software. As such, it
only supports the SLURM scheduler, via the supplied slurmit script. We
plan to provide support for other schedulers in the near future.

Installing NanoOK RT
--------------------
NanoOK RT is included within the core NanoOK software. However, if you are only using NanoOK RT, you do not need to install all the dependencies. Please make sure you have installed the following:

1. `HDF5 tools <https://www.hdfgroup.org/products/hdf5_tools/>`_ - are required for extracting FASTA/Q from FAST5 files.
2. `BLAST <https://www.st-va.ncbi.nlm.nih.gov/blast/Blast.cgi?CMD=Web&PAGE_TYPE=BlastDocs&DOC_TYPE=Download>`_ and a suitable database.

If using Mac OS, these can easily be installed with `Homebrew <https://brew.sh/>`_. On Linux, you can use a package manager.

NanoOK can be downloaded from GitHub. You can either download the .zip file, or if you have git installed, you can type::
  git clone https://github.com/TGAC/NanoOK.git

To install:
1. Copy the whole NanoOK directory somewhere appropriate on your system.2. Set a NANOOK_DIR environment variable to point to this directory. This enables NanoOK to know where to find the Java JARs and R scripts it uses. On Linux or MacOS, you would typically do this by adding the following command to your .bash_profile (or .profile on Ubuntu) file or 'source' script::   
     export NANOOK_DIR=/path/to/NanoOK   On MacOs, you can edit the file using::   
     open -e ~/.bash_profile3. Add the bin directory to your PATH variable. On Linux, you would typically do this by adding the following command to your .bash_profile (or .profile on Ubuntu) file or 'source' script::     export PATH=/path/to/NanoOK/bin:$PATH4. You may have to reset for the change to take effect.5. Set Java heap size for the tool - the main JAVA program is packaged as a .jar file. A wrapper script called nanook (in the bin directory) enables this to be run without specifying the Java Virtual Machine parameters. By default, this script request 2Gb of RAM for the Java heap. If you wish to reduce/increase this, please edit this file.

Running NanoOK RT
-----------------

To run NanoOK RT, you must specify the 'rt' option to nanook and specify
a process file, for example::

  nanook rt -t 8 -process processfile.txt -log log.txt -timeout 10000 -templateonly

where:

-  ``-t`` specifies the number of execution threads to use.
-  ``-process`` specifies the name of a process file (see below).
-  ``-log`` optionally specifies a log file.
-  ``-timeout`` sets the time after which NanoOK will exit if it hasn't seen a new read to process.
-  ``-templateonly`` specifies only process template reads.

Process files
-------------

Process files define the actions that NanoOK RT will carry out for each
read. An example is shown below::

  Sample:BAMBI\_P8\_2D\_Local\_070317 Extract:fasta
  Fast5Dir:/tgac/workarea/group-si/BAMBI\_Pt4/BAMBI\_P8\_2D\_Local\_070317/basecalled
  ReadsPerBlast:500
  Blast:bacteria,blastn,/tgac/references/databases/blast/nt\_04022017/bacteria\_human\_28022016,8G,tgac-medium
  Blast:card,blastn,/tgac/workarea/group-si/BAMBI\_Pt1/CARD\_1.1.1\_Download\_17Oct16/nucleotide\_fasta\_protein\_homolog\_model.fasta,8G,TempProject4
  Blast:nt,blastn,/tgac/references/databases/blast/nt\_04022017/nt,16G,TempProject4

The line beginning 'Sample:' defines the sample directory - this can be
relative from the current directory (this case) or absolute (beginning
with a '/').

The next section defines read extraction. The Extract line asks for
FASTA extraction and the Fast5Dir specifies the location of the fast5
files (like the -f option of standard NanoOK).

**Note: NanoOK RT looks at the fast5 directory structure in order to
work out the origin of the basecalled .fast5 files (Metrichor, MinKNOW,
Albacore), so you must either create this structure before running
NanoOK RT, or only run NanoOK RT after some reads have been output.**

The final section defines BLAST processes. The ReadsPerBlast defines the
chunk size. Then each Blast process consists of the following::

  Blast:identifier,blast_tool,/path/to/database,SLURM_memory_requirements,SLURM_partition_name
