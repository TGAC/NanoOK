/*
 * Program: NanoOK
 * Author:  Richard M. Leggett (richard.leggett@earlham.ac.uk)
 * 
 * Copyright 2015-17 Earlham Institute
 */

package nanook;

import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Representation of program options and some global constants.
 * 
 * @author Richard Leggett
 */
public class NanoOKOptions implements Serializable {
    private static final long serialVersionUID = NanoOK.SERIAL_VERSION;
    public final static int MAX_KMER = 20000;
    public final static int MAX_READ_LENGTH = 1000000;
    public final static int MAX_READS = 1000000;
    public final static int MODE_EXTRACT = 1;
    public final static int MODE_ALIGN = 2;
    public final static int MODE_ANALYSE = 3;
    public final static int MODE_COMPARE = 4;
    public final static int MODE_WATCH = 5;
    public final static int MODE_PROCESS = 6;
    public final static int FASTA = 1;
    public final static int FASTQ = 2;
    public final static int TYPE_TEMPLATE = 0;
    public final static int TYPE_COMPLEMENT = 1;
    public final static int TYPE_2D = 2;
    public final static int TYPE_ALL = -1;
    public final static int TYPE_INSERTION = 0;
    public final static int TYPE_DELETION = 1;
    public final static int TYPE_SUBSTITUTION = 2;
    public final static int READTYPE_COMBINED = 0;
    public final static int READTYPE_PASS = 1;
    public final static int READTYPE_FAIL = 2;
    public final static int MIN_ALIGNMENTS = 10;
    public final static int PROGRESS_WIDTH = 50;
    private References references = new References(this);
    private String referenceFile=null;
    private String sampleDirectory = null;
    private String sampleName = null;
    private String scriptsDir="/Users/leggettr/Documents/github/nanotools/scripts";
    private String aligner="last";
    private String alignerParams="";
    private String scheduler="system";
    private String sampleList = null;
    private String comparisonDir = null;
    private String bacteriaPath = null;
    private String ntPath = null;
    private String cardPath = null;
    private String processFile = null;
    private int coverageBinSize = 100;
    private boolean processPassReads = true;
    private boolean processFailReads = true;
    private boolean processSkipReads = true;
    private boolean parseAlignments = true;
    private boolean plotGraphs = true;
    private boolean makeReport = true;
    private boolean makePDF = true;
    private int maxReads = 0;
    private boolean process2DReads = true;
    private boolean processTemplateReads = true;
    private boolean processComplementReads = true;
    private boolean fixIDs = false;
    private boolean fixRandom = false;
    private boolean doKmerCounting = true;
    private boolean showAlignerCommand = false;
    private boolean extractingReads = false;
    private boolean convertingFastQ = false;
    private boolean aligningReads = false;
    private boolean parsingReads = false;
    private boolean blastingReads = false;
    private boolean mergeFastaFiles = false;
    private boolean force = false;
    private double minQForPass = -1;
    private int maxSchedulerJobs = 4;
    private int runMode = 0;
    private int readFormat = FASTA;
    private int numThreads = 1;
    private int fileWatcherTimeout = 10;
    private String jobQueue = "";
    private NanoOKLog logFile = new NanoOKLog();
    private String imageFormat = "pdf";
    private int specifiedType = TYPE_2D;
    private String readsDir = "fast5";
    private String fastQConvertDir = "fastq_pass";
    private int returnValue = 0;
    private int basecallIndex = -1;
    private boolean outputFast5Path = true;
    private int readsPerBlast = 500;
    private boolean clearLogsOnStart = true;
    private SimpleJobScheduler jobScheduler = null;
    private transient WatcherLog watcherReadLog = new WatcherLog(this);
    private transient WatcherLog watcherCardFileLog = new WatcherLog(this);
    private transient WatcherLog watcherntFileLog = new WatcherLog(this);
    private transient WatcherLog watcherCardCommandLog = new WatcherLog(this);
    private transient WatcherLog watcherntCommandLog = new WatcherLog(this);
    private transient BlastMerger mergerCardPass = new BlastMerger(this);
    private transient BlastMerger mergerntPass = new BlastMerger(this);
    private transient BlastMerger mergerCardFail = new BlastMerger(this);
    private transient BlastMerger mergerntFail = new BlastMerger(this);
    private transient MergedFastAQFile mergedPass2D;
    private transient MergedFastAQFile mergedPass1D;
    private transient MergedFastAQFile mergedFail1D;
    private transient MergedFastAQFile mergedFail2D;
    private transient ThreadPoolExecutor executor;
    private transient BlastHandler[][] blastHandlers = new BlastHandler[3][2];
    private transient ArrayList<String> blastProcesses = new ArrayList<String>();
    private int fileCounterOffset = 0;
    private transient ReadFileMerger readFileMerger;
    private transient SampleChecker sampleChecker = new SampleChecker(this);
    private double blastMaxE = 0.001;
    private int blastMaxTargetSeqs = 25;
    private boolean isMac = false;
    private String meganCmdLine="source MEGAN-5.11.3 ; /tgac/software/testing/MEGAN/5.11.3/x86_64/xvfb-run -d MEGAN";
    private String meganLicense="/tgac/software/testing/MEGAN/5.11.3/x86_64/megan/MEGAN5-academic-license.txt";
    private boolean nedome = false;
    private NedomeProcessor nedProcess = null;
    private boolean nedProcessorRunning = false;
    private String nedomeFile = "";
        
    public NanoOKOptions() {
        String value = System.getenv("NANOOK_DIR");
        
        if (value != null) {
            scriptsDir = value + File.separator + "bin";
        } else {
            System.out.println("*** WARNING: You should set NANOOK_DIR. Default value unlikely to work. ***");
            System.out.println("");
        }
                
        System.out.println("Scripts dir: "+scriptsDir);
        
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.startsWith("mac os x")) { 
            isMac = true;
            System.out.println("Mac OS X detected");
            meganCmdLine="/Applications/MEGAN5.11.3/MEGAN.app/Contents/MacOS/JavaApplicationStub";
            meganLicense="/Applications/MEGAN5/MEGAN5-academic-license.txt";
        }
        
        System.out.println("To do: options for specifying MEGAN command/license");
    }
    
    public boolean isNedProcessorRunning() {
        return nedProcessorRunning;
    }
    
    public void setNedProcessorRunning(boolean tf) {
        nedProcessorRunning = tf;
    }
    
    public void updateNedome() {        
        if (nedProcessorRunning == false) {            
            nedProcessorRunning = true;
            executor.execute(new NedomeProcessor(this));
        }
    }
    
    public boolean isNedome() {
        return nedome;
    }
    
    public boolean isMac() {
        return isMac;
    }
    
    public String getMeganCmdLine() {
        return meganCmdLine;
    }
    
    public String getMeganLicense() {
        return meganLicense;
    }
    
    public References getReferences() {
        return references;
    }
    
    public void setReferences(References r) {
        references = r;
    }
    
    public void setReturnValue(int r) {
        returnValue = r;
    }
    
    public int getReturnValue() {
        return returnValue;
    }
    
    /**
     * Parse command line arguments.
     * @param args array of command line arguments
     */
    public void parseArgs(String[] args) {
        int i=0;
        
        if (args.length <= 1) {
            System.out.println("");
            System.out.println("Syntax nanook <extract|align|analyse|compare|rt> [options]");
            System.out.println("");
            System.out.println("extract options:");
            System.out.println("    -s|-sample <dir> specifies sample directory");
            System.out.println("    -f|-reads specifies alternative dir for FAST5 files (default fast5)");
            System.out.println("              Can be absolute (beginning with /) or relative");
            System.out.println("              e.g. -f reads/downloads if replicating Metrichor file structure");
            System.out.println("    -a|-fasta specifies FASTA file extraction (default)");
            System.out.println("    -q|-fastq specifies FASTQ file extraction");
            System.out.println("    -basecallindex specifies the index of the analysis (default: latest)");
            //System.out.println("    -printpath to output FAST5 path in FASTA read header");
            System.out.println("    -mergereads to generate merged FASTA files in addition to single read files");
            System.out.println("    -minquality <value> to set the minimum quality for a 'pass' read");
            System.out.println("");
            System.out.println("align options:");
            System.out.println("    -s|-sample <dir> specifies sample directory");
            System.out.println("    -r|-reference <path> specifies path to reference database");
            System.out.println("    -aligner <name> specifies the aligner (default last)"); 
            System.out.println("    -alignerparams <params> specifies paramters to the aligner");
            System.out.println("    -showaligns echoes aligner commands to screen");
            System.out.println("    -a|-fasta specifies FASTA file input (default)");
            System.out.println("    -q|-fastq specifies FASTQ file input");
            System.out.println("");
            System.out.println("analyse options:");
            System.out.println("    -s|-sample <dir> specifies sample directory");
            System.out.println("    -r|-reference <path> specifies path to reference database");
            System.out.println("    -aligner <name> specifies the aligner (default last)");            
            System.out.println("    -coveragebin <int> specifies coverage bin size (default 100)");            
            System.out.println("    -bitmaps to output bitmap PNG graphs instead of PDF");
            System.out.println("");
            System.out.println("compare options:");
            System.out.println("    -l|-samplelist <file> specifies a sample list file");
            System.out.println("    -o|-outputdir <directory> specifies an output directory");
            System.out.println("    -type <2d|template|complement> specifies an output directory");
            System.out.println("");
            System.out.println("rt options:");
            System.out.println("    -process <file> specifies a process file");
            System.out.println("");
            //System.out.println("Sample type options:");
            //System.out.println("    -barcoding if reads are barcoded and sorted into subdirs");
            //System.out.println("    -batchdirs if using MinKNOW 1.4.2 or above with separate batch_ directories");
            //System.out.println("");
            System.out.println("Read type options:");
            System.out.println("    -passonly to analyse only pass reads");
            System.out.println("    -failonly to analyse only fail reads");            
            System.out.println("    -2donly to analyse only 2D reads"); 
            System.out.println("    -templateonly to analyse just Template reads"); 
            System.out.println("    -complementonly to analyse just Complement reads"); 
            System.out.println("");
            System.out.println("Other options:");
            System.out.println("    -t|-numthreads <number> specifies the number of threads to use (default 1)");
            System.out.println("    -log <filename> enables debug logging to file");
            System.out.println("    -force to force NanoOK to ignore warnings");
            System.out.println("    -timeout to set the number of seconds before giving up waiting for new reads (default 2)");
            System.out.println("");
            System.out.println("Valid aligners: last, bwa, blasr, marginalign, graphmap, minimap2, ngmlr");
            System.out.println("");
            System.exit(0);
        }
        
        parseAlignments = true;
        plotGraphs = true;
        makeReport = true;
                        
        if (args[i].equals("extract")) {
            runMode = MODE_EXTRACT;
            extractingReads = true;
            aligningReads = false;
            parsingReads = false;
            blastingReads = false;
            mergeFastaFiles = false;
            fileWatcherTimeout = 2;
        } else if (args[i].equals("align")) {
            runMode = MODE_ALIGN;
            extractingReads = false;
            aligningReads = true;
            parsingReads = false;
            blastingReads = false;
            mergeFastaFiles = false;
            fileWatcherTimeout = 2;
        } else if (args[i].equals("analyse") || args[i].equals("analyze")) {
            runMode = MODE_ANALYSE;
            extractingReads = false;
            aligningReads = false;
            parsingReads = true;
            blastingReads = false;
            mergeFastaFiles = false;
            fileWatcherTimeout = 2;
        } else if (args[i].equals("compare")) {
            runMode = MODE_COMPARE;
        } else if (args[i].equals("watch")) {
            runMode = MODE_WATCH;
        } else if ((args[i].equals("process")) || (args[i].equals("scan")) || (args[i].equals("rt"))) {
            runMode = MODE_PROCESS;
        } else {
            System.out.println("Unknonwn mode " + args[i] + " - must be extract, align or analyse");
            System.exit(1);
        }
        i++;
        
        while (i < (args.length)) {
            if (args[i].equalsIgnoreCase("-coveragebin")) {
                coverageBinSize = Integer.parseInt(args[i+1]);
                i+=2;
            } else if (args[i].equalsIgnoreCase("-batchdirs")) {
                System.out.println("-batchdirs option ignore - now detected automatically.");
                i++;
            } else if (args[i].equalsIgnoreCase("-timeout")) {
                fileWatcherTimeout = Integer.parseInt(args[i+1]);
                i+=2;
            } else if (args[i].equalsIgnoreCase("-fileoffset")) {
                fileCounterOffset = Integer.parseInt(args[i+1]);
                i+=2;
            } else if (args[i].equalsIgnoreCase("-reference") || args[i].equalsIgnoreCase("-r")) {
                referenceFile = args[i+1];
                i+=2;
            } else if (args[i].equalsIgnoreCase("-process")) {
                processFile = args[i+1];
                readProcessFile();
                i+=2;
            } else if (args[i].equalsIgnoreCase("-bacteria")) {
                bacteriaPath = args[i+1];
                i+=2;
            } else if (args[i].equalsIgnoreCase("-nt")) {
                ntPath = args[i+1];
                i+=2;
            } else if (args[i].equalsIgnoreCase("-card")) {
                cardPath = args[i+1];
                i+=2;
            } else if (args[i].equalsIgnoreCase("-force")) {
                force = true;
                i++;
            } else if (args[i].equalsIgnoreCase("-sample") |  args[i].equalsIgnoreCase("-s")) {
                sampleDirectory = args[i+1];
                i+=2;
            } else if (args[i].equalsIgnoreCase("-samplelist") |  args[i].equalsIgnoreCase("-l")) {
                sampleList = args[i+1];
                i+=2;
            } else if (args[i].equalsIgnoreCase("-outputdir") |  args[i].equalsIgnoreCase("-o")) {
                comparisonDir = args[i+1];
                i+=2;
            } else if (args[i].equalsIgnoreCase("-reads") |  args[i].equalsIgnoreCase("-f")) {
                readsDir = args[i+1];
                if (readsDir.endsWith("/")) {
                    readsDir = readsDir.substring(0, readsDir.length()-1);
                }
                i+=2;
            } else if (args[i].equalsIgnoreCase("-maxreads")) {
                maxReads = Integer.parseInt(args[i+1]);
                i+=2;
            } else if (args[i].equalsIgnoreCase("-log")) {
                logFile.open(args[i+1]);
                i+=2;
            } else if (args[i].equalsIgnoreCase("-type")) {
                if (args[i+1].equalsIgnoreCase("template")) {
                    specifiedType = TYPE_TEMPLATE;
                } else if (args[i+1].equalsIgnoreCase("complement")) {
                    specifiedType = TYPE_COMPLEMENT;
                } else if (args[i+1].equalsIgnoreCase("2d")) {
                    specifiedType = TYPE_2D;
                }
                i+=2;
            } else if (args[i].equalsIgnoreCase("-nofail") || args[i].equalsIgnoreCase("-passonly")) {
                processPassReads = true;
                processFailReads = false;  
                i++;
            } else if (args[i].equalsIgnoreCase("-nopass") || args[i].equalsIgnoreCase("-failonly")) {
                processPassReads = false;
                processFailReads = true;
                i++;
            } else if (args[i].equalsIgnoreCase("-fasta") || args[i].equalsIgnoreCase("-a")) {
                //if (runMode == MODE_EXTRACT) { 
                    readFormat = FASTA;
                //}
                i++;
            } else if (args[i].equalsIgnoreCase("-fastq") || args[i].equalsIgnoreCase("-q")) {
                //if (runMode == MODE_EXTRACT) { 
                    readFormat = FASTQ;
                //}
                i++;
            } else if (args[i].equalsIgnoreCase("-2donly")) {
                process2DReads = true;
                processTemplateReads = false;
                processComplementReads = false;
                i++;
            } else if ((args[i].equalsIgnoreCase("-1d")) || 
                       (args[i].equalsIgnoreCase("-templateonly")) ) {
                process2DReads = false;
                processTemplateReads = true;
                processComplementReads = false;
                i++;
            } else if (args[i].equalsIgnoreCase("-complementonly")) {
                process2DReads = false;
                processTemplateReads = false;
                processComplementReads = true;
                i++;                
            } else if (args[i].equalsIgnoreCase("-printpath")) {
                outputFast5Path = true;
                i++;
            } else if (args[i].equalsIgnoreCase("-bitmaps")) {
                imageFormat = "png";
                i++;
            } else if (args[i].equalsIgnoreCase("-fixids")) {
                fixIDs = true;
                i++;
            } else if (args[i].equalsIgnoreCase("-showaligns")) {
                showAlignerCommand = true;
                i++;
            } else if (args[i].equalsIgnoreCase("-deterministic")) {
                fixRandom = true;
                i++;                
            } else if (args[i].equalsIgnoreCase("-aligner")) {
                aligner = args[i+1];
                i+=2;
            } else if (args[i].equalsIgnoreCase("-alignerparams")) {
                alignerParams = args[i+1];
                System.out.println("Alignment parameters: "+alignerParams);
                i+=2;
            } else if (args[i].equalsIgnoreCase("-scheduler")) {
                scheduler = args[i+1];
                i+=2;
            } else if (args[i].equalsIgnoreCase("-queue")) {
                jobQueue = args[i+1];
                i+=2;
            } else if (args[i].equalsIgnoreCase("-readsperblast")) {
                readsPerBlast = Integer.parseInt(args[i+1]);
                i+=2;
            } else if (args[i].equalsIgnoreCase("-basecallindex")) {
                basecallIndex = Integer.parseInt(args[i+1]);
                i+=2;
            } else if (args[i].equalsIgnoreCase("-numthreads") || args[i].equalsIgnoreCase("-t")) {
                numThreads = Integer.parseInt(args[i+1]);
                i+=2;
            } else if (args[i].equalsIgnoreCase("-subdirs") || args[i].equalsIgnoreCase("-barcoding")) {
                System.out.println("-barcoding option ignored - now detected automatically.");
                i++;
            } else if (args[i].equalsIgnoreCase("-keeplogs")) {
                clearLogsOnStart = false;
                i++;
            } else if (args[i].equalsIgnoreCase("-mergereads")) {
                mergeFastaFiles = true;
                i++;
            } else if (args[i].equalsIgnoreCase("-minquality")) {
                minQForPass = Double.parseDouble(args[i+1]);
                i+=2;
            } else if (args[i].equalsIgnoreCase("-maxe")) {
                blastMaxE = Double.parseDouble(args[i+1]);
                i+=2;
            } else if (args[i].equalsIgnoreCase("-blastmaxtargets")) {
                blastMaxTargetSeqs = Integer.parseInt(args[i+1]);
                i+=2;
            } else {                
                System.out.println("Unknown parameter: " + args[i]);
                System.exit(0);
            }            
        }
                
        if ((runMode == MODE_ALIGN) || (runMode == MODE_ANALYSE)) {
            if (referenceFile == null) {
                System.out.println("Error: You must specify a reference");
                System.exit(1);
            }

            if (!referenceFile.endsWith(".fa") && !referenceFile.endsWith(".fasta")) {
                System.out.println("Error: reference must specify a .fa or .fasta file");
                System.exit(1);
            }
            
            checkParser();
        }
        
        if (runMode == MODE_PROCESS) {
            if (processFile == null) {
                System.out.println("Error: you must specify a process file");
                System.exit(1);
            }
        }
        
        if (runMode == MODE_COMPARE) {
            if (comparisonDir == null) {
                System.out.println("Error: you must specify an output dir for the comparison");
                System.exit(1);
            } else {
                checkAndMakeComparisonDirs();
            }
        } else {
            if (sampleDirectory == null) {
                System.out.println("Error: You must specify a sample");
                System.exit(1);
            } else {
                File s = new File(sampleDirectory);
                if (!s.exists()) {
                    System.out.println("Error: sample directory doesn't exist");
                    System.exit(1);
                }

                if (!s.isDirectory()) {
                    System.out.println("Error: sample doesn't point to a directory");
                    System.exit(1);
                }

                sampleDirectory = s.getAbsolutePath();

                sampleName = s.getName();
            }
        }
        
        if (scheduler.equalsIgnoreCase("local")) {
            checkAndMakeDirectory(this.getLogsDir());
            jobScheduler = new SimpleJobScheduler(maxSchedulerJobs, this);
        }
                
        initialiseBlastHandlers();
        
        //System.out.println("Number of cores: "+Runtime.getRuntime().availableProcessors());
        
        executor = new ThreadPoolExecutor(numThreads, numThreads, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    }    
    
    public String getAligner() {
        return aligner;
    }
    
    public String getAlignerParams() {
        return alignerParams;
    }
    
    public void setReadFormat(int f) {
        readFormat = f;
        //System.out.println("Read format "+f);
    }
        
    /**
     * Get sample name.
     * @return name String
     */
    public String getSample() {
        return sampleName;
    }
    
    /**
     * Get name of references file.
     * @return filename String
     */
    public String getReferenceFile() {
        return referenceFile;
    }
    
    /**
     * Get coverage graph bin size.
     * @return bin size
     */
    public int getCoverageBinSize() {
        return coverageBinSize;
    }
        
    /**
     * Get a type string (Template, Complement, 2D) from an integer.
     * @param n integer to convert
     * @return type String
     */
    public static String getTypeFromInt(int n) {
        String typeString;
        
        switch(n) {
            case TYPE_TEMPLATE: typeString = "Template"; break;
            case TYPE_COMPLEMENT: typeString = "Complement"; break;
            case TYPE_2D: typeString = "2D"; break;
            default: typeString = "Unknown"; break;
        }
        
        return typeString;
    }
    
    public static String getPassFailFromInt(int n) {
        String typeString;
        
        switch(n) {
            case READTYPE_PASS: typeString = "pass"; break;
            case READTYPE_FAIL: typeString = "fail"; break;
            default: typeString = "Unknown"; break;
        }
        
        return typeString;
    }

    /**
     * Get an error type string (Insertion, Deletion, Substitution) from an integer.
     * @param n error type integer
     * @return type String
     */
    public static String getErrorTypeFromInt(int n) {
        String typeString;
        
        switch(n) {
            case TYPE_INSERTION: typeString = "Insertion"; break;
            case TYPE_DELETION: typeString = "Deletion"; break;
            case TYPE_SUBSTITUTION: typeString = "Substitution"; break;
            default: typeString = "Unknown"; break;
        }
        
        return typeString;
    }
        
    /**
     * Check if various required directories exist and create if not.
     */
    public void checkAnalysisDirectoryStructure() {
        File analysisDir = new File(getAnalysisDir());
        File unalignedAnalysisDir = new File(getAnalysisDir()+File.separator+"Unaligned");
        File graphsDir = new File(getGraphsDir());
        File motifsDir = new File(getGraphsDir() + File.separator + "motifs");
        File latexDir = new File(getLatexDir());
        
        if (!analysisDir.exists()) {
            analysisDir.mkdir();
        }
        
        if (!unalignedAnalysisDir.exists()) {
            unalignedAnalysisDir.mkdir();
        }

        if (!graphsDir.exists()) {
            graphsDir.mkdir();
        }    

        if (!motifsDir.exists()) {
            motifsDir.mkdir();
        }    
        
        if (!latexDir.exists()) {
            latexDir.mkdir();
        }    
    }

    public void checkAndMakeComparisonDirs() {
        File f = new File(comparisonDir);
        if (!f.exists()) {
            f.mkdir();
        }
        
        f = new File(this.getGraphsDir());
        if (!f.exists()) {
            f.mkdir();
        }

        f = new File(this.getLatexDir());
        if (!f.exists()) {
            f.mkdir();
        }
        
        f = new File(this.getLogsDir());
        if (!f.exists()) {
            f.mkdir();
        }        

        f = new File(this.getLogsDir()+File.separator+"R");
        if (!f.exists()) {
            f.mkdir();
        }            
    }
    
    /**
     * Check if an analysis reference directory exists and make if not.
     * @param reference name of reference
     */
    public void checkAndMakeReferenceAnalysisDir(String reference) {
        File analysisDir = new File(getAnalysisDir() + File.separator + reference);
        File graphsDir = new File(getGraphsDir() + File.separator + reference);
        
        if (!analysisDir.exists()) {
            analysisDir.mkdir();
        }
        if (!graphsDir.exists()) {
            graphsDir.mkdir();
        }

    }
    
    /**
     * Create a new alignment summary file.
     */
    public void initialiseAlignmentSummaryFile() {
        try {
            PrintWriter pw = new PrintWriter(new FileWriter(this.getAlignmentSummaryFilename())); 
            pw.close();
        } catch (IOException e) {
            System.out.println("initialiseAlignmentSummaryFile exception:");
            e.printStackTrace();
            System.exit(1);
        }        
    }
    
    /**
     * Get filename of alignment summary file.
     * @return filename String
     */
    public String getAlignmentSummaryFilename() {
        return getAnalysisDir() + File.separator + "all_summary.txt";
    }

    /**
     * Get filename of length summary file.
     * @return filename String
     */
    public String getLengthSummaryFilename() {
        return getAnalysisDir() + File.separator + "length_summary.txt";
    }    
    
    /**
     * Get scripts directory.
     * @return directory name as String
     */
    public String getScriptsDir() {
        return scriptsDir;
    }
    
    public String getSampleDirectory() {
        return sampleDirectory;
    }
    
    /**
     * Get graphs directory.
     * @return directory name as String
     */
    public String getGraphsDir() {
        if (runMode == MODE_COMPARE) {
            return comparisonDir + File.separator + "graphs";
        } else {
            return sampleDirectory + File.separator + "graphs" + getAnalysisSuffix();
        }
    } 

    public String getFastaDir() {
        return sampleDirectory + File.separator + "fasta";
    }

    public String getFastqDir() {
        return sampleDirectory + File.separator + "fastq";
    }    
    
    public String getFast5Dir() {
        // Check for full path
        if ((readsDir.startsWith("/")) || (readsDir.startsWith("~")) || (readsDir.startsWith("."))) {
            return readsDir;
        } else {
            return sampleDirectory + File.separator + readsDir;
        }
    }
    
    public String getFastQConvertDir() {
        return fastQConvertDir;
    }
    
    /**
     * Get FASTA directory.
     * @return directory name as String
     */
    public String getReadDir() {
        String dir;
        
        if (readFormat == FASTQ) {
            dir = getFastqDir();
        } else {
            dir = getFastaDir(); 
        }
                
        return dir;
    } 
    
    public String getExpectedReadFormat() {
        String format;
        
        if (readFormat == FASTQ) {
            format = "FASTQ";
        } else {
            format = "FASTA"; 
        }
        
        return format;
    }

    /**
     * Get LAST directory.
     * @return directory name as String
     */
    public String getAlignerDir() {
        return sampleDirectory + File.separator + aligner;
    } 

    /**
     * Get LAST directory.
     * @return directory name as String
     */
    public String getParserDir() {
        return sampleDirectory + File.separator + aligner+"_parse";
    }     
    
    /**
     * Get LaTeX directory.
     * @return directory name as String
     */
    public String getLatexDir() {
        if (runMode == MODE_COMPARE) {
            return comparisonDir + File.separator + "latex" + getAnalysisSuffix();
        } else {
            return sampleDirectory + File.separator + "latex" + getAnalysisSuffix();
        }
    } 

    /**
     * Get logs directory.
     * @return directory name as String
     */
    public String getLogsDir() {
        if (runMode == MODE_COMPARE) {
            return comparisonDir + File.separator + "logs";
        } else {
            return sampleDirectory + File.separator + "logs";
        }
    } 
    
    //public boolean isPassFailFast5Dir() {
    //    File passDir = new File(getFast5Dir() + File.separator + "pass");
    //    File failDir = new File(getFast5Dir() + File.separator + "fail");
    //    boolean rc = false;
        
    //    if (((passDir.exists() && passDir.isDirectory()) || (failDir.exists() && failDir.isDirectory()))) {
    //        rc = true;
    //    }
        
    //    return rc;
    //}
    


    //public boolean isPassFailReadDir() {
    //    File passDir = new File(getReadDir() + File.separator + "pass");
    //    File failDir = new File(getReadDir() + File.separator + "pass");
    //    boolean rc = false;
    //    
    //    if (((passDir.exists() && passDir.isDirectory()) || failDir.exists() && failDir.isDirectory())) {
    //        rc = true;
    //    }
    //    
    //    
    //    
    //    return rc;
    //}
    
    public String getAnalysisSuffix() {
        String s = new String("_"+aligner);
        if (processPassReads && processFailReads) {
            s += "_passfail";
        } else if (processPassReads) {
            s += "_passonly";
        } else if (processFailReads) {
            s += "_failonly";
        }
        
        if (!processTemplateReads && !processComplementReads) {
            s += "_2donly";
        }
        
        return s;
    }
    
    
    /**
     * Get analysis directory.
     * @return directory name as String
     */
    public String getAnalysisDir() {
        return sampleDirectory + File.separator + "analysis" + getAnalysisSuffix();
    } 
    
    /**
     * Get LaTeX filename.
     * @return filename as String
     */
    public String getTexFilename() {
        return sampleDirectory + File.separator + "latex" + getAnalysisSuffix() + File.separator + sampleName + ".tex";
    }
    
    /**
     * Check if processing "pass" reads.
     * @return true to process
     */
    public boolean isProcessingPassReads() {
        return processPassReads;
    }

    /**
     * Check if processing "fail" reads.
     * @return true to process
     */
    public boolean isProcessingFailReads() {
        return processFailReads;
    }

    /**
     * Check if processing "skip" reads.
     * @return true to process
     */
    public boolean isProcessingSkipReads() {
        return processSkipReads;
    }
    
    public boolean isProcessingComplementReads() {
        return processComplementReads;
    }
    
    public boolean isProcessingTemplateReads() {
        return processTemplateReads;
    }

    public boolean isProcessing2DReads() {
        return process2DReads;
    }
    
    public boolean isProcessingReadType(int type) {
        boolean r = false;
        
        switch(type) {
            case TYPE_ALL:
                r = true;
                break;
            case TYPE_TEMPLATE:
                r = processTemplateReads;
                break;
            case TYPE_COMPLEMENT:
                r = processComplementReads;
                break;
            case TYPE_2D:
                r = process2DReads;
                break;
        }         
        
        return r;
    }
    
    public int getNumberOfTypes() {
        int t = 0;
        if (processTemplateReads) t++;
        if (processComplementReads) t++;
        if (process2DReads) t++;
        return t;
    }
    
    /**
     * Check if to parse alignments or not
     * @return true to parse
     */
    public boolean doParseAlignments() {
        return parseAlignments;
    }
    
    /**
     * Check if to plot graphs or not
     * @return true to plot
     */
    public boolean doPlotGraphs() {
        return plotGraphs;
    }
    
    /**
     * Check if to make report or not
     * @return true to make report
     */
    public boolean doMakeReport() {
        return makeReport;
    }

    /**
     * Check if to make report or not
     * @return true to make report
     */
    public boolean doMakePDF() {
        return makePDF;
    }
        
    /**
     * Get maximum number of reads (used for debugging)
     * @return maximum number of reads
     */
    public int getMaxReads() {
        return maxReads;
    }
    
    public int getReadFormat() {
        return readFormat;
    }
        
    public int getRunMode() {
        return runMode;
    }
    
    public boolean fixIDs() {
        return fixIDs;
    }
    
    public boolean fixRandom() {
        return fixRandom;
    }
    
    public String getScheduler() {
        return scheduler;
    }
    
    public int getNumberOfThreads() {
        return numThreads;
    }
    
    public String getQueue() {
        return jobQueue;
    }
    
    public NanoOKLog getLog() {
        return logFile;
    }
    
    public boolean isBarcoded() {
        return sampleChecker.usingBarcodes();
    }
    
    /**
     * Get the right parser
     * @param options
     * @return 
     */
    public AlignmentFileParser getParser() {
        AlignmentFileParser parser = null;
        
        switch(aligner) {
            case "last":
                parser = new LastParser(this, references);
                break;
            case "bwa":
                parser = new BWAParser(this, references);                
                break;
            case "blasr":
                parser = new BLASRParser(this, references);                                    
                break;
            case "marginalign":
                parser = new MarginAlignParser(this, references);                                    
                break;
            case "graphmap":
                parser = new GraphMapParser(this, references);
                break;
            case "minimap2":
                parser = new Minimap2Parser(this, references);
                break;
            case "ngmlr":
                parser = new NgmlrParser(this, references);
                break;
            default:
                System.out.println("Aligner unknown!");
                System.out.println("");
                System.exit(1);
                break;                      
        }
        
        if (alignerParams != "") {
            parser.setAlignmentParams(alignerParams);
        }
                
        return parser;
    }    
    
    public void checkParser() {
        AlignmentFileParser parser = getParser();
        parser.checkForIndex(getReferenceFile().substring(0, getReferenceFile().lastIndexOf('.')));
    }
    
    public boolean doKmerCounting() {
        return doKmerCounting;
    }
    
    public String getImageFormat() {
        return imageFormat;
    }
    
    public String getSampleList() {
        return sampleList;
    }
    
    public String getComparisonDir() {
        return comparisonDir;
    }
    
    public int getSpecifiedType() {
        return specifiedType;
    }
    
    public boolean showAlignerCommand() {
        return showAlignerCommand;
    }
    
    public int getBasecallIndex() {
        return basecallIndex;
    }
    
    public boolean outputFast5Path() {
        return outputFast5Path;
    }    

    public String getBacteriaPath() {
        if (bacteriaPath == null) {
            System.out.println("Error: no nt path set.\n");
            System.exit(1);
        }
        
        return bacteriaPath;
    }

    public String getntPath() {
        if (ntPath == null) {
            System.out.println("Error: no nt path set.\n");
            System.exit(1);
        }
        
        return ntPath;
    }    
    
    public String getCardPath() {
        if (cardPath == null) {
            System.out.println("Error: no CARD path set.\n");
            System.exit(1);
        }
        
        return cardPath;
    }
    
    public WatcherLog getWatcherReadLog() {
        return watcherReadLog;
    }

    public WatcherLog getWatcherCardFileLog() {
        return watcherCardFileLog;
    }
    
    public WatcherLog getWatcherCardCommandLog() {
        return watcherCardCommandLog;
    }

    public WatcherLog getWatcherntFileLog() {
        return watcherntFileLog;
    }
    
    public WatcherLog getWatcherntCommandLog() {
        return watcherntCommandLog;
    }
    
    public BlastMerger getMergerCardPass() {
        return mergerCardPass;
    }
    
    public BlastMerger getMergerntPass() {
        return mergerntPass;
    }

    public BlastMerger getMergerCardFail() {
        return mergerCardFail;
    }
    
    public BlastMerger getMergerntFail() {
        return mergerntFail;
    }
    
    
    public boolean clearLogsOnStart() {
        return clearLogsOnStart;
    }
    
    public void openMergedFile(String filename, int type, int pf) {
        if (pf == NanoOKOptions.READTYPE_PASS) {
            if (type == NanoOKOptions.TYPE_TEMPLATE) {
                mergedPass1D = new MergedFastAQFile(this, filename);
            } else if (type == NanoOKOptions.TYPE_2D) {
                mergedPass2D = new MergedFastAQFile(this, filename);
            }
        } else if (pf == NanoOKOptions.READTYPE_FAIL) {
            if (type == NanoOKOptions.TYPE_TEMPLATE) {
                mergedFail1D = new MergedFastAQFile(this, filename);    
            } else if (type == NanoOKOptions.TYPE_2D) {
                mergedFail2D = new MergedFastAQFile(this, filename);
            }
        }
    }

    public MergedFastAQFile getMergedFile(int type, int pf) {
        if (pf == NanoOKOptions.READTYPE_PASS) {
            if (type == NanoOKOptions.TYPE_TEMPLATE) {
                return mergedPass1D;
            } else if (type == NanoOKOptions.TYPE_2D) {
                return mergedPass2D;
            }
        } else if (pf == NanoOKOptions.READTYPE_FAIL) {
            if (type == NanoOKOptions.TYPE_TEMPLATE) {
                return mergedFail1D;    
            } else if (type == NanoOKOptions.TYPE_2D) {
                return mergedFail2D;
            }
        }
        
        return null;
    }

    public int getReadsPerBlast() {
        return readsPerBlast;
    }
    
    public ThreadPoolExecutor getThreadExecutor() {
        return executor;
    }
    
    public boolean keepRunning() {
        return true;
    }
    
    public boolean isExtractingReads() {
        return extractingReads;
    }
    
    public boolean isConvertingFastQ() {
        return convertingFastQ;
    }
    
    public boolean isAligningRead() {
        return aligningReads;
    }

    public boolean isParsingRead() {
        return parsingReads;
    }    
    
    public boolean isBlastingRead() {
        return blastingReads;
    }
    
    public int getFileWatcherTimeout() {
        return fileWatcherTimeout;
    }

    private void checkAndMakeDirectory(String dir) {
        File f = new File(dir);
        if (f.exists()) {
            if (!f.isDirectory()) {
                System.out.println("Error: " + dir + " is a file, not a directory!");
                System.exit(1);
            }
        } else {
            System.out.println("Making directory " + dir);
            f.mkdir();
        }
    }      

    private void checkAndMakeDirectoryWithChildren(String dirname) {
        checkAndMakeDirectory(dirname);
        for (int t=0; t<3; t++) {
            if (this.isProcessingReadType(t)) {
                checkAndMakeDirectory(dirname + File.separator + NanoOKOptions.getTypeFromInt(t));
            }
        }        
    }
    
    // Directory structure
    // fast5
    //     - pass
    //         - BC01
    //         - BC02
    //     - fail
    //         - unaligned
    // fasta
    //     - pass
    //         - BC01
    //             - 2D
    //             - Template
    //             - Complement
    //         - BC02
    //         ...
    //     - fail
    public void makeDirectories() {
        checkAndMakeDirectory(this.getLogsDir());
        
        if (this.isExtractingReads()) {
            checkAndMakeDirectory(this.getReadDir());
            
            //if (this.isNewStyleDir()) {
            //    for (int i=READTYPE_PASS; i<=READTYPE_FAIL; i++) {
            //        String pf = NanoOKOptions.getPassFailFromInt(i);
            //        checkAndMakeDirectoryWithChildren(this.getReadDir() + File.separator + pf);
            //        if (this.processSubdirs()) {
            //            File inputDir = new File(this.getFast5Dir());
            //            File[] listOfFiles = inputDir.listFiles();
            //            for (File file : listOfFiles) {
            //                if (file.isDirectory()) {
            //                    checkAndMakeDirectoryWithChildren(this.getReadDir() + File.separator + file.getName());
            //                }
            //            }
            //        }
            //    }
            //}                
        }

        if (this.isConvertingFastQ()) {
            checkAndMakeDirectory(this.getFastaDir());
        }
        
        if (this.isBlastingRead()) {
            checkAndMakeDirectory(this.getReadDir() + "_chunks");
        }
        
        if (this.isAligningRead()) {
            checkAndMakeDirectory(this.getAlignerDir());
            checkAndMakeDirectory(this.getLogsDir() + File.separator + this.getAligner());
            //if (this.isNewStyleReadDir()) {
            //    for (int i=READTYPE_PASS; i<=READTYPE_FAIL; i++) {
            //        String pf = NanoOKOptions.getPassFailFromInt(i);
            //        checkAndMakeDirectoryWithChildren(this.getAlignerDir() + File.separator + pf);
            //        checkAndMakeDirectoryWithChildren(this.getLogsDir() + File.separator + this.getAligner() + File.separator + pf);
            //        if (this.processSubdirs()) {
            //            File inputDir = new File(this.getReadDir());
            //            File[] listOfFiles = inputDir.listFiles();
            //            for (File file : listOfFiles) {
            //                if (file.isDirectory()) {
            //                    checkAndMakeDirectoryWithChildren(this.getAlignerDir() + File.separator + file.getName());
            //                    checkAndMakeDirectoryWithChildren(this.getLogsDir() + File.separator + this.getAligner() + File.separator + file.getName());
            //                }
            //            }
            //       }
            //    }
            //}
        }
     
        if (this.isParsingRead()) {
            checkAndMakeDirectory(this.getParserDir());
            //if (this.isNewStyleReadDir()) {
            //    for (int i=READTYPE_PASS; i<=READTYPE_FAIL; i++) {
            //        String pf = NanoOKOptions.getPassFailFromInt(i);
            //        checkAndMakeDirectoryWithChildren(this.getParserDir() + File.separator + pf);
            //        if (this.processSubdirs()) {
            //            File inputDir = new File(this.getReadDir());
            //            File[] listOfFiles = inputDir.listFiles();
            //            for (File file : listOfFiles) {
            //                if (file.isDirectory()) {
            //                    checkAndMakeDirectoryWithChildren(this.getParserDir() + File.separator + file.getName());
            //                }
            //            }
            //        }
            //    }
            //}                        
        }
        
        if (this.isBlastingRead()) {
            for (int i=0; i<blastProcesses.size(); i++) {
                String[] params = blastProcesses.get(i).split(",");
                if (params.length == 5) {
                    String blastName = params[0];
                    String blastTool = params[1];
                    String blastDb = params[2];
                    String memory = params[3];
                    String queue = params[4];
                    checkAndMakeDirectory(getSampleDirectory() + File.separator + blastTool + "_" + blastName + File.separator);
                    checkAndMakeDirectory(getLogsDir() + File.separator + blastTool + "_" + blastName + File.separator);
                }
            }
        }
    }
    
    void readProcessFile() {
        BufferedReader br;
        
        System.out.println("\nReading process file "+processFile);
        try {
            br = new BufferedReader(new FileReader(processFile));        
            String line;

            do {
                line = br.readLine();
                if (line != null) {
                    if (line.length() > 1) {
                        String[] tokens = line.split(":");
                        if (tokens[0].compareToIgnoreCase("Extract") == 0) {
                            extractingReads = true;
                            System.out.println("  Extract "+tokens[1]);
                        } else if (tokens[0].compareToIgnoreCase("Fast5Dir") == 0) {
                            readsDir = tokens[1];
                            System.out.println("  Fast5Dir "+tokens[1]);
                        } else if (tokens[0].compareToIgnoreCase("ConvertFastQ") == 0) {
                            convertingFastQ = true;
                            fastQConvertDir = tokens[1];
                            System.out.println("  FastQDir "+fastQConvertDir);
                        } else if (tokens[0].compareToIgnoreCase("Aligner") == 0) {
                            aligningReads = true;
                            System.out.println("  Aligner "+tokens[1]);
                        } else if (tokens[0].compareToIgnoreCase("Reference") == 0) {
                            referenceFile = tokens[1];
                            System.out.println("  Reference "+tokens[1]);
                        } else if (tokens[0].compareToIgnoreCase("Sample") == 0) {
                            sampleDirectory = tokens[1];
                            System.out.println("  Sample "+tokens[1]);
                        } else if (tokens[0].compareToIgnoreCase("Analysis") == 0) {
                            parsingReads = true;
                            System.out.println("  Analysis "+tokens[1]);
                        } else if (tokens[0].compareToIgnoreCase("Aligner") == 0) {
                            aligner = tokens[1];
                            System.out.println("  Aligner "+tokens[1]);
                        } else if (tokens[0].compareToIgnoreCase("Blast") == 0) {
                            blastingReads = true;
                            blastProcesses.add(tokens[1]);
                            System.out.println("  Blast "+tokens[1]);
                        } else if (tokens[0].compareToIgnoreCase("ReadsPerBlast") == 0) {
                            readsPerBlast = Integer.parseInt(tokens[1]);
                            System.out.println("  ReadsPerBlast "+readsPerBlast);
                        } else if (tokens[0].compareToIgnoreCase("MaxEValue") == 0) {
                            blastMaxE = Double.parseDouble(tokens[1]);
                            System.out.println("  MaxEValue "+blastMaxE);
                        } else if (tokens[0].compareToIgnoreCase("MaxTargetSeqs") == 0) {
                            blastMaxTargetSeqs = Integer.parseInt(tokens[1]);
                            System.out.println("  MaxTargetSeqs "+blastMaxTargetSeqs);
                        } else if (tokens[0].compareToIgnoreCase("Nedome") == 0) {
                            System.out.println("  Nedome");
                            nedomeFile = tokens[1];
                            nedome = true;
                        } 
                        else if (!tokens[0].startsWith("#")) {
                            System.out.println("Unknown token "+tokens[0]);
                        } 
                    }
                }
            } while (line != null);
        } catch (Exception e) {
            System.out.println("readProcessFile Exception:");
            e.printStackTrace();
            System.exit(1);
        }
        
        System.out.println("");
    }
    
    public void initialiseBlastHandlers() {
        for (int t=0; t<3; t++) {
            for (int pf=NanoOKOptions.READTYPE_PASS; pf<=NanoOKOptions.READTYPE_FAIL; pf++) {
                blastHandlers[t][pf-1] = new BlastHandler(this, t, pf);
            }
        }
    }
    
    public BlastHandler getBlastHandler(int t, int pf) {
        return blastHandlers[t][pf-1];
    }
    
    public ArrayList<String> getBlastProcesses() {
        return blastProcesses;
    }
    
    public int getFileCounterOffset() {
        return fileCounterOffset;
    }
    
    public boolean mergeFastaFiles() {
        return mergeFastaFiles;
    }
    
    public ReadFileMerger getReadFileMerger() {
        return readFileMerger;
    }
    
    public boolean usingBatchDirs() {
        return sampleChecker.usingBatchDirs();
    }
    
    public boolean doForce() {
        return force;
    }
    
    public SampleChecker getSampleChecker() {
        return sampleChecker;
    }
    
    public boolean usingPassFailDirs() {
        return sampleChecker.usingPassFailDirs();
    }
    
    public double getMinQ() {
        return minQForPass;
    }
    
    public void initialiseReadMerger() {
        readFileMerger = new ReadFileMerger(this);
    }
    
    public boolean debugMode() {
        return false;
    }
    
    public SimpleJobScheduler getJobScheduler() {
        return jobScheduler;
    }
    
    public double getBlastMaxE() {
        return blastMaxE;
    }
    
    public int getBlastMaxTargetSeqs() {
        return blastMaxTargetSeqs;
    }
    
    public String getNedomeFile() {
        return nedomeFile;
    }
}
