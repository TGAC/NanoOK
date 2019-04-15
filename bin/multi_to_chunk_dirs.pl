#!/usr/bin/perl -w

use Getopt::Long;
use File::Path;

my $in_filename;
my $out_filename;
my $out_prefix;
my $removewhitespace;
my $out_dir;
my $is_fastq;
my $is_fasta;
my $suffix;
my $reads_per_dir=4000;

&GetOptions(
    'in|i:s'            => \$in_filename,
    'out|o:s'           => \$out_prefix,
    'fastq|q'           => \$is_fastq,
    'fasta|a'           => \$is_fasta,
    'reads_per_dir|n:i' => \$reads_per_dir
);

die "Can't be FASTQ and FASTA!\n"
    if (defined $is_fastq) && (defined $is_fasta);

die "Syntax: split_to_separate_files.pl -in <filename> -out <sample directory>\n"
    if (not defined $in_filename) || (not defined $out_prefix);

if (($in_filename =~ /.fq$/) || ($in_filename =~ /.fastq$/)) {
    print "Detected FASTQ file extension\n";
    $is_fastq = 1;
} elsif (($in_filename =~ /.fa$/) || ($in_filename =~ /.fasta$/)) {
    print "Detected FASTA file extension\n";
    $is_fasta = 1;
}

if (defined $is_fasta) {
    $suffix="fasta";
} elsif (defined $is_fastq) {
    $suffix="fastq";
} else {
    die "You must specify -fastq or -fasta\n";
}

my $file_open=0;
my $read_ctr=0;
my $chunk_ctr=0;
$out_dir=$out_prefix."/".$chunk_ctr;
print "Making ".$out_dir."\n";
mkpath($out_dir);

open(INFILE, $in_filename) or die "Can't open input file $in_filename\n";
while(<INFILE>) {
    my $line = $_;
    my $new_read_id;

    if (defined $is_fasta) {
        if ($line =~ /^>(\S+)/) {
            $new_read_id = $1;
        }
     } elsif (defined $is_fastq) {
         if ($line =~ /^@(\S+)/) {
            $new_read_id = $1;
        }
     }

    if (defined $new_read_id) {
        if ($file_open) {
            close(OUTFILE);
            $read_ctr++;

            if ($read_ctr == $reads_per_dir) {
                $chunk_ctr++;
                $read_ctr=0;
                $out_dir=$out_prefix."/".$chunk_ctr;
                print $out_dir."\n";
                mkdir($out_dir);
            }
        }

        my $out_file=$out_dir."/".$new_read_id.".".$suffix;
        print "Writing ".$out_file."\n";
        open(OUTFILE, ">".$out_file) or die "Can't open output file $out_file\n";
        $file_open = 1;
    }

    if ($file_open) {        
        print OUTFILE $line; 
    }
}

close(INFILE);

if ($file_open) {
    close(OUTFILE);
}
