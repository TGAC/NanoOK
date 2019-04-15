/*
 * Program: NanoOK
 * Author:  Richard M. Leggett (richard.leggett@earlham.ac.uk)
 * 
 * Copyright 2015-17 Earlham Institute
 */

package nanook;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
//import ncsa.hdf.object.FileFormat;
//import ncsa.hdf.object.h5.H5File;

/**
 * Entry class for tool.
 * 
 * @author Richard Leggett
 */
public class NanoOK {
    public final static String VERSION_STRING = "v1.42";
    public final static long SERIAL_VERSION = 3L;
    
    /**
     * Check for program dependencies - R, pdflatex
     */
    public static void checkDependencies() {
        ProcessLogger pl = new ProcessLogger();
        ArrayList<String> response;
        String rVersion = null;
        String pdflatexVersion = null;
        String hVersion = null;
                
        response = pl.checkCommand("Rscript --version");
        if (response != null) {
            for (int i=0; i<response.size(); i++) {
                String s = response.get(i);
                if (s.startsWith("R scripting front-end")) {
                    rVersion = s;
                }
            }
        }
        
        if (rVersion == null) {
            System.out.println("*** ERROR: Couldn't find Rscript - is R installed? ***");
        } else {
            System.out.println(rVersion);
        }
        
        response = pl.checkCommand("pdflatex --version");
        if (response != null) {
            for (int i=0; i<response.size(); i++) {
                String s = response.get(i);
                if (s.contains("pdfTeX")) {
                    pdflatexVersion = s;
                    break;
                }
            }
        }
        
        if (pdflatexVersion == null) {
            System.out.println("*** ERROR: Couldn't find pdflatex - is TeX installed? ***");
        } else {
            System.out.println(pdflatexVersion);
        }

        response = pl.checkCommand("h5dump --version");
        if (response != null) {
            for (int i=0; i<response.size(); i++) {
                String s = response.get(i);
                if (s.startsWith("h5dump")) {
                    hVersion = s;
                }
            }
        }
        
        if (hVersion == null) {
            System.out.println("*** ERROR: Couldn't find h5dump - is H5 Tools installed? ***");
        } else {
            System.out.println(hVersion);
        }
        
        //try {
        //    H5File file = new H5File();
        //} catch (NoClassDefFoundError | UnsatisfiedLinkError e) {
        //    e.printStackTrace();
        //    System.out.println("");
        //    System.out.println("Error: Could not initialise HDF5 classes. Check that the HDF libraries are correctly installed (and pointed to by LD_LIBRARY_PATH or DYLD_LIBRARY_PATH).");
        //    System.out.println("Consult HDF documentation and/or NanoOK documentation.");
        //    System.out.println("");
        //    System.exit(1);
        //}
                           
        System.out.println("");
    }

    /**
     * Test logo plotting
     */
    public static void testLogo() {
        SequenceLogo logo = new SequenceLogo();
        logo.drawImage();
        logo.saveImage("/Users/leggettr/Desktop/logo.png");
    }
    
    /**
     * Test SequenceReader class
     */
    public static void testSequenceReader() {
        SequenceReader r = new SequenceReader(true);
        r.indexFASTAFile("/Users/leggettr/Documents/Projects/Nanopore/test.fasta", null, true);
        String s = r.getSubSequence("gi|223667766|ref|NZ_DS264586.1|", 0, 499);
        System.out.println("String (0,499) = ["+s+"]");
        s = r.getSubSequence("gi|223667766|ref|NZ_DS264586.1|", 0, 9);
        System.out.println("String (0,9) = ["+s+"]");
        s = r.getSubSequence("gi|223667766|ref|NZ_DS264586.1|", 200, 209);
        System.out.println("String (200,209) = ["+s+"]");
        s = r.getSubSequence("gi|223667766|ref|NZ_DS264586.1|", 200, 214);
        System.out.println("String (200,214) = ["+s+"]");
    }
    
    public static void testSamToLast(NanoOKOptions options, References references) {
        BWAParser parser = new BWAParser(options, references);
        AlignmentsTableFile nonAlignedSummaryFile = new AlignmentsTableFile("atf.txt");
        ReadSetStats readSetStats = new ReadSetStats(options, NanoOKOptions.TYPE_2D);
        options.getReferences().loadReferences();
        parser.parseFile("/Users/leggettr/Desktop/test.fasta.sam", nonAlignedSummaryFile, readSetStats);
    }
    
    /**
     * Test parser
     * @param options
     * @param overallStats
     * @param references 
     */
    public static void testParser(NanoOKOptions options, OverallStats overallStats, References references) {
        AlignmentFileParser p = new LastParser(options, references);
        AlignmentsTableFile nonAlignedSummary = new AlignmentsTableFile("blob.txt");
        //p.parseFile("/Users/leggettr/Documents/Projects/Nanopore/N79681_EvenMC_R7_06082014/last/2D/N79681_EvenMC_R7_0608215_5314_1_ch319_file116_strand.fast5_BaseCalled_2D.fasta.maf", nonAlignedSummary, overallStats);
        //System.exit(0);
    }
    
    /**
     * Test HDF5 library
     */
    public static void testHDF(NanoOKOptions options) {
        //ReadExtractorRunnable r = new ReadExtractorRunnable(options, null, null, null);        
        //String fastq = r.getFastq("/Users/leggettr/Desktop/TEST12345_ch1_file0.fast5", NanoOKOptions.TYPE_TEMPLATE);        

        Fast5File f = new Fast5File(options, "/Users/leggettr/Desktop/TEST12345_ch1_file0.fast5");
        Fast5File g = new Fast5File(options, "/Users/leggettr/Documents/Projects/Nanopore/NanoOK_lambda_test/fast5/pass/N79596_Lambda8kbp_LCv4_test_3559_1_ch37_file38_strand.fast5");
        FastAQFile ff = f.getFastq(-1, NanoOKOptions.TYPE_TEMPLATE);
        FastAQFile fg = g.getFastq(-1, NanoOKOptions.TYPE_TEMPLATE);
        if (ff != null) {
            ff.writeFastq("ff.fq");
        }
        
        if (fg != null) {
            fg.writeFastq("fg.fq");
        }
        //f.printGroups();
        System.exit(0);        
    }
    
    private static void analyse(NanoOKOptions options) throws InterruptedException {
        OverallStats overallStats = new OverallStats(options);
        options.getReferences().setOverallStats(overallStats);

        options.getSampleChecker().checkReadDirectory();
        
        // Load reference data
        options.getReferences().loadReferences();
        options.setReadFormat(options.getParser().getReadFormat());
        options.initialiseAlignmentSummaryFile();
        
        System.out.println("");
        
        // Parse all reads sets       
        if (options.doParseAlignments()) {
            ReadLengthsSummaryFile summary = new ReadLengthsSummaryFile(options.getLengthSummaryFilename());
            summary.open(options.getSample());
            
            for (int type = 0; type<3; type++) {
                if (options.isProcessingReadType(type)) {
                    System.out.println("Parsing " + NanoOKOptions.getTypeFromInt(type));
                    ReadSet readSet = new ReadSet(type, options, overallStats.getStatsByType(type));
                    int nReads = readSet.processReads();

                    if (nReads < 1) {
                        System.out.println("Error: unable to find any " + NanoOKOptions.getTypeFromInt(type) + " reads to process.");
                        System.out.println("");
                        System.exit(1);
                    }

                    int nReadsWithAlignments = readSet.getStats().getNumberOfReadsWithAlignments();
                    if (nReadsWithAlignments < 1) {
                        System.out.println("");
                        System.out.println("Error: unable to find any " + NanoOKOptions.getTypeFromInt(type) + " alignments to process.");
                        System.out.println("Common reasons for this:");
                        System.out.println("1. Failure to index the reference with the alignment tool, resulting in alignment files of 0 bytes");
                        System.out.println("2. Wrong reference specified to the align stage, resulting in no alignments");
                        System.out.println("3. When indexing with LAST, the output prefix needs to be the same as the reference FASTA file, minus the .fasta extension");
                        System.out.println("   e.g. lastdb -Q 0 referencename referencename.fasta");
                        System.out.println("");
                        System.exit(1);
                    } else if (nReadsWithAlignments < 400) {
                        System.out.println("Warning: not many alignments ("+nReadsWithAlignments+") found to process.");
                    }

                    summary.addReadSetStats(overallStats.getStatsByType(type));
                    overallStats.getStatsByType(type).closeKmersFile();
                    overallStats.getStatsByType(type).writeSubstitutionStats();
                    overallStats.getStatsByType(type).writeErrorMotifStats();
                    
                    int ignoredDuplicates = overallStats.getStatsByType(type).getIgnoredDuplicates();
                    if (ignoredDuplicates > 0) {
                        System.out.println(ignoredDuplicates + " ignored duplicate read IDs.");
                    }
                    
                    System.out.println("");
                    
                }
            }
            summary.close();            
            
            // Write files
            System.out.println("Writing analysis files");
            Set<String> ids = options.getReferences().getAllIds();
            int allCount = 3; //ids.size() * 3;
            int counter = 1;            
            for (int type=0; type<3; type++) {
                long completed = counter;
                long total = allCount;
                long e = 0;
                long s = NanoOKOptions.PROGRESS_WIDTH;

                if (total > 0) {
                    e = NanoOKOptions.PROGRESS_WIDTH * completed / total;
                    s = NanoOKOptions.PROGRESS_WIDTH - e;
                }
                                
                System.out.print("\r[");
                for (int i=0; i<e; i++) {
                    System.out.print("=");
                }
                for (int i=0; i<s; i++) {
                    System.out.print(" ");
                }
                System.out.print("] " + completed +"/" +  total);                
                options.getReferences().writeReferenceStatFiles(type);
                options.getReferences().writeReferenceSummary(type);
                counter++;
            }
            System.out.println("");

            System.out.println("Writing object");
            try {
                FileOutputStream fos = new FileOutputStream(options.getAnalysisDir() + File.separator + "OverallStats.ser");
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(overallStats);
                oos.close();
            } catch (Exception e) {
                System.out.println("Exception trying to write object:");
                e.printStackTrace();
            }
        
        }
        
        // Plot graphs
        if (options.doPlotGraphs()) {
            System.out.println("");
            System.out.println("Plotting graphs");
            RGraphPlotter plotter = new RGraphPlotter(options);
            plotter.plot(false);                
        }
        
        // Make report
        if (options.doMakeReport()) {
            System.out.println("");
            System.out.println("Making report");
            SampleReportWriter rw = new SampleReportWriter(options, overallStats);
            rw.writeReport();

            if (options.doMakePDF()) {
                System.out.println("");
                System.out.println("Making PDF");
                rw.makePDF();
            }
        }
                
        System.out.println("");
        System.out.println("Done");
    }
    
    private static void extract(NanoOKOptions options) throws InterruptedException {
        ReadExtractor re = new ReadExtractor(options);
        re.createDirectories();
        re.extract();
    }
    
    private static void align(NanoOKOptions options) throws InterruptedException {
        AlignmentFileParser parser = options.getParser();
        parser.checkForIndex(options.getReferenceFile().substring(0, options.getReferenceFile().lastIndexOf('.')));
        ReadAligner aligner = new ReadAligner(options, parser);
        options.setReadFormat(parser.getReadFormat());
        aligner.createDirectories();
        aligner.align();
    }
    
    private static void compare(NanoOKOptions options) throws InterruptedException {
        System.out.println("Comparing");
        SampleComparer comparer = new SampleComparer(options);
        comparer.loadSamples();
        comparer.compareSamples();
        
        options.setReferences(comparer.getSample(0).getStatsByType(0).getOptions().getReferences());

        System.out.println("");
        System.out.println("Plotting graphs");
        RGraphPlotter plotter = new RGraphPlotter(options);
        plotter.plot(true);   
        
        System.out.println("");
        System.out.println("Making PDF");
        ComparisonReportWriter crw = new ComparisonReportWriter(options, comparer);
        crw.writeReport();
        crw.makePDF();
    }
    
    private static void watch(NanoOKOptions options) throws InterruptedException {
        AlignmentFileParser parser = options.getParser();
        parser.checkForIndex(options.getReferenceFile().substring(0, options.getReferenceFile().lastIndexOf('.')));
        ReadAligner aligner = new ReadAligner(options, parser);
        options.setReadFormat(parser.getReadFormat());
        aligner.createDirectories();

        DirectoryWatcher dw = new DirectoryWatcher(options, aligner, parser);
        dw.watch();
    }
    
    private static void process(NanoOKOptions options) throws InterruptedException {
        ReadProcessor rp = new ReadProcessor(options);
        options.makeDirectories();      
        options.initialiseReadMerger();
        rp.process();
    }    
    
    private static void memoryReport() {
        Runtime runtime = Runtime.getRuntime();
        long mb = 1024 * 1024;
        long totalMem = runtime.totalMemory() / mb;
        long maxMem = runtime.maxMemory() / mb;
        long freeMem = runtime.freeMemory() / mb;
        System.out.println("totalMem: " + totalMem + "Mb");
        System.out.println("  maxMem: " + maxMem + "Mb");
        System.out.println(" freeMem: " + freeMem + "Mb");
    }
    
    /**
     * Entry to tool.
     * @param args command line arguments
     */
    public static void main(String[] args) throws InterruptedException {
        System.out.println("");
        System.out.println("NanoOK " + VERSION_STRING);
        System.out.println("");
        System.out.println("Comments/bugs to: richard.leggett@earlham.ac.uk");
        System.out.println("Follow NanoOK on twitter: @NanoOK_Software");
        System.out.println("");

        NanoOKOptions options = new NanoOKOptions();
               
        Locale.setDefault(new Locale("en", "US"));
        
        // Parse command line
        options.parseArgs(args);

        // Check dependencies
        System.out.println("");
        System.out.println("Checking dependencies");
        checkDependencies();
        
        //testHDF(options);
        //System.exit(0);

        File logsDir = new File(options.getLogsDir());
        if (!logsDir.exists()) {
            logsDir.mkdir();
        }
        
        //NedomeProcessor np = new NedomeProcessor(options);
        //np.run();
        //System.exit(1);
        
        
        if (options.getRunMode() == NanoOKOptions.MODE_EXTRACT) {
            //extract(options);
            process(options);
        } else if (options.getRunMode() == NanoOKOptions.MODE_ALIGN) {
            //align(options);
            process(options);
        } else if (options.getRunMode() == NanoOKOptions.MODE_ANALYSE) {
            options.checkAnalysisDirectoryStructure();
            analyse(options);
            //scan(options);
        } else if (options.getRunMode() == NanoOKOptions.MODE_COMPARE) {
            compare(options);
        } else if (options.getRunMode() == NanoOKOptions.MODE_WATCH) {
            watch(options);
        } else if (options.getRunMode() == NanoOKOptions.MODE_PROCESS) {
            process(options);
        }
        
        //memoryReport();
        
        options.getLog().close();
        
        options.getThreadExecutor().shutdown();
        
        if (options.getReturnValue() != 0) {
            System.out.println("Exiting with error code");
            System.exit(options.getReturnValue());
        }
    }
}
