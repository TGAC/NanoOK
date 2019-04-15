/*
 * Program: NanoOK
 * Author:  Richard M. Leggett (richard.leggett@earlham.ac.uk)
 * 
 * Copyright 2015-17 Earlham Institute
 */

package nanook;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enable multi-threading of read extraction
 * 
 * @author Richard Leggett
 */
public class ReadProcessorRunnable implements Runnable {
    public final static String TYPE_STRING_TEMPLATE = "/Analyses/Basecall_2D_000/BaseCalled_template/Fastq";
    public final static String TYPE_STRING_COMPLEMENT = "/Analyses/Basecall_2D_000/BaseCalled_complement/Fastq";
    public final static String TYPE_STRING_2D = "/Analyses/Basecall_2D_000/BaseCalled_2D/Fastq";
    private String[] typeStrings = {TYPE_STRING_TEMPLATE, TYPE_STRING_COMPLEMENT, TYPE_STRING_2D};
    public NanoOKOptions options;
    public FileWatcher fileWatcher;
    public boolean isNewStyleDir;
    
    public ReadProcessorRunnable(NanoOKOptions o, FileWatcher f) {
        options = o;
        fileWatcher = f;
    }   
    
    //rivate String getFastaqDirFromFast5Name(String fast5Pathname, int type) {
    //    File f = new File(fast5Pathname);        
    //    String inDir = f.getParent();
    //    String outDir = options.getReadDir();
        
    //    if (!inDir.startsWith(options.getFast5Dir())) {
    //        System.out.println("Something wrong with fast5 filename - shouldn't get to this code. Please contact richard.leggett@earlham.ac.uk");
    //        System.exit(1);
    //    }
        
        // If using batch dirs, then we go sample/fasta/2D/pass/batch_XXX
        // If using old style, then we go sample/fasta/pass/2D
        //if (options.usingBatchDirs()) {
    //        outDir = outDir + File.separator + NanoOKOptions.getTypeFromInt(type) + inDir.substring(options.getFast5Dir().length());
        //} else {       
        //    outDir = outDir + inDir.substring(options.getFast5Dir().length()) + File.separator + NanoOKOptions.getTypeFromInt(type);
        //}
        
        //options.getLog().println("     In: "+fast5Pathname);
        //options.getLog().println(" OutDir: "+outDir);
        
    //    return outDir;
    //}/    

    private String getAlignmentPathnameFromFastaqName(String fastaqPathname) {
        File f = new File(fastaqPathname);        
        String inDir = f.getPath();
        String outPathname;
        
        if (!fastaqPathname.startsWith(options.getReadDir())) {
            System.out.println("Something wrong with read filename - shouldn't get to this code. Please contact richard.leggett@earlham.ac.uk");
            System.out.println("FastaPathname: "+fastaqPathname);
            System.out.println("ReadDir: "+options.getReadDir());
            System.exit(1);
        }
        
        outPathname = options.getAlignerDir() + inDir.substring(options.getReadDir().length());
        File outFile = new File(outPathname);
        File parent = new File(outFile.getParent());
        
        if (!parent.exists()) {
            options.getLog().println("Making directory " + parent.getPath());
            parent.mkdirs();
        }        
        
        return outPathname;
    }      

    private String getAlignmentLogPathnameFromFastaqName(String fastaqPathname) {
        File f = new File(fastaqPathname);        
        String inDir = f.getPath();
        String outPathname;
        
        if (!fastaqPathname.startsWith(options.getReadDir())) {
            System.out.println("Something wrong with read filename - shouldn't get to this code. Please contact richard.leggett@earlham.ac.uk");
            System.out.println("FastaPathname: "+fastaqPathname);
            System.out.println("ReadDir: "+options.getReadDir());
            System.exit(1);
        }
        
        outPathname = options.getLogsDir() + File.separator + options.getAligner() + inDir.substring(options.getReadDir().length());
        File outFile = new File(outPathname);
        File parent = new File(outFile.getParent());
        
        if (!parent.exists()) {
            options.getLog().println("Making directory " + parent.getPath());
            parent.mkdirs();
        }        
        
        return outPathname;
    }      
    
    private String getParserPathnameFromAlignmentName(String alignmentPathname) {
        File f = new File(alignmentPathname);        
        String inDir = f.getPath();
        String outPathname;
        
        if (!alignmentPathname.startsWith(options.getAlignerDir())) {
            System.out.println("Something wrong with read filename - shouldn't get to this code. Please contact richard.leggett@earlham.ac.uk");
            System.exit(1);
        }
        
        outPathname = options.getParserDir() + inDir.substring(options.getAlignerDir().length());
        File outFile = new File(outPathname);
        File parent = new File(outFile.getParent());
        
        if (!parent.exists()) {
            options.getLog().println("Making directory " + parent.getPath());
            parent.mkdirs();
        }        
        
        return outPathname;
    }          

    private String getFastaqPathnameFromAlignmentName(String alignmentPathname) {
        File f = new File(alignmentPathname);        
        String inDir = f.getPath();
        String outPathname;
        
        if (!alignmentPathname.startsWith(options.getAlignerDir())) {
            System.out.println("Something wrong with read filename - shouldn't get to this code. Please contact richard.leggett@earlham.ac.uk");
            System.exit(1);
        }
        
        outPathname = options.getReadDir() + inDir.substring(options.getAlignerDir().length(),inDir.lastIndexOf('.'));
                
        return outPathname;
    }      
    
    public static String getFilePrefixFromPathname(String pathname) {
        File f = new File(pathname);
        String inName = f.getName();
        int suffixPos = inName.lastIndexOf(".");
        String outName;
        
        if (suffixPos > 0) {
            outName = inName.substring(0, suffixPos);
        } else {
            outName = inName;
        }

        //options.getLog().println("OutName: "+outName);

        return outName;
    }
    
    private void runCommandLocal(String command, String outPath) {
        ProcessLogger pl = new ProcessLogger();
        
        // outPath only non-null if aligner will only write to screen (yes, BWA, I'm talking about you)
        if (outPath != null) {
            pl.setWriteFormat(false, true, false);
            pl.runAndLogCommand(command, outPath, false);
        } else {
            pl.runCommand(command);
        }
    }    

    public void runParse(String alignmentPathname) {
        String parsedPathname = getParserPathnameFromAlignmentName(alignmentPathname) + ".txt";
        String fastaqPathname = getFastaqPathnameFromAlignmentName(alignmentPathname);
        ReadParser rp = new ReadParser(options);
        
        options.getLog().println("Parsing file "+ alignmentPathname);
        options.getLog().println("          to "+ parsedPathname);
        
        rp.parse(fastaqPathname, alignmentPathname, parsedPathname);
    }
    
    public void runAlign(String fastaqPathname) {
        String reference = options.getReferenceFile();
        AlignmentFileParser parser = options.getParser();
        
        String filePrefix = getFilePrefixFromPathname(fastaqPathname);
        String alignmentPathname = getAlignmentPathnameFromFastaqName(fastaqPathname) + parser.getAlignmentFileExtension();
        String alignmentLogPathname = getAlignmentLogPathnameFromFastaqName(fastaqPathname);

        options.getLog().println("Aligning file "+fastaqPathname);        
        options.getLog().println("           to "+alignmentPathname);        
        options.getLog().println("     with log "+alignmentLogPathname);        
        
        String command = parser.getRunCommand(fastaqPathname, alignmentPathname, reference);                            
        if (options.showAlignerCommand()) {
            System.out.println("Running: " + command);
        }
        runCommandLocal(command, parser.outputsToStdout() ? alignmentPathname:null);
        if (options.isParsingRead()) {
            runParse(alignmentPathname);
        }
    }
    
    public void runConvertFastQ(String fastqPathname) {
        File f = new File(fastqPathname);
        String fastqLeafname = f.getName();
        String fastaPathname = options.getFastaDir() + "_chunks/"+fastqLeafname.substring(0,fastqLeafname.lastIndexOf('.'))+".fasta";
        options.getLog().println("Converting "+fastqPathname);        
        options.getLog().println("        to "+fastaPathname);
        try {
            String header;
            PrintWriter pw = new PrintWriter(new FileWriter(fastaPathname));        
            BufferedReader br = new BufferedReader(new FileReader(fastqPathname));
            while ((header = br.readLine()) != null) {
                if (header.startsWith("@")) {
                    String seq = br.readLine();
                    String plus = br.readLine();
                    String qual = br.readLine();
                    if (plus.equals("+")) {
                        pw.println(">"+header.substring(1));
                        pw.println(seq);
                    } else {
                        System.out.println("ERROR: Badly formatted FASTQ entry in "+fastqPathname);
                    }
                } else {
                    System.out.println("ERROR: Badly formatted FASTQ file: "+fastqPathname);
                }
            }
            br.close();
            pw.close();
        } catch (IOException e) {
            System.out.println("runConvertFastQ exception");
            e.printStackTrace();
        }
        
        options.getBlastHandler(NanoOKOptions.TYPE_TEMPLATE, NanoOKOptions.READTYPE_PASS).addReadChunk(fastaPathname);
    }
    
    public void addToBlast(String fastaqPathname, int type) {
        int pf = NanoOKOptions.READTYPE_PASS;
        
        if (fastaqPathname.contains("/fail/")) {
            pf = NanoOKOptions.READTYPE_FAIL;
        }
        
        options.getBlastHandler(type, pf).addRead(fastaqPathname);
    }
    
    private String getFastaqFilename(String fast5Pathname, int t, int inputPF, int outputPF) {
        File f = new File(fast5Pathname);        
        String inDir = f.getParent();
        String suffixDirs;
        
        if (!inDir.startsWith(options.getFast5Dir())) {
            System.out.println("Something wrong with fast5 filename - shouldn't get to this code. Please contact richard.leggett@earlham.ac.uk");
            System.exit(1);
        }
          
        if (inputPF == NanoOKOptions.READTYPE_COMBINED) {
            suffixDirs = inDir.substring(options.getFast5Dir().length());
        } else {
            // +5 for /pass or /fail
            suffixDirs = inDir.substring(options.getFast5Dir().length() + 5);            
        }
        
        String fastaqDir = options.getReadDir() + File.separator;
        if (outputPF == NanoOKOptions.READTYPE_FAIL) {
            fastaqDir += "fail";
        } else {
            fastaqDir += "pass";
        }

        fastaqDir += File.separator + NanoOKOptions.getTypeFromInt(t) + suffixDirs;
        File dir = new File(fastaqDir);
        
        String filePrefix = getFilePrefixFromPathname(fast5Pathname);
        String fileExtension = options.getReadFormat() == NanoOKOptions.FASTA ? ".fasta":".fastq";       
        
        if (!dir.exists()) {
            options.getLog().println("Making directory " + fastaqDir);
            dir.mkdirs();
        }        
        
        String fastaqPathname = fastaqDir + File.separator + filePrefix + "_BaseCalled_" + NanoOKOptions.getTypeFromInt(t) + fileExtension;            
        
        return fastaqPathname;
    }
    
    public void runExtract(String fast5Pathname, int inputPF) {
        Fast5File inputFile = new Fast5File(options, fast5Pathname);
        int outputPF;
                
        options.getLog().println("Extracting file "+fast5Pathname);

        for (int t=0; t<3; t++) {
            if (options.isProcessingReadType(t)) {
                FastAQFile ff = inputFile.getFastq(options.getBasecallIndex(), t);
                double meanQ = 0;
                                
                if (ff != null) {
                    // If pass/fail not assigned, default to pass directory output
                    if (inputPF == NanoOKOptions.READTYPE_COMBINED) {
                        outputPF = NanoOKOptions.READTYPE_PASS;
                    } else {
                        outputPF = inputPF;
                    }

                    // Have we set a min quality threshold? In which case, test...
                    meanQ = inputFile.getMeanQ(options.getBasecallIndex(), t);
                    if (options.getMinQ() >= 0) {
                        if (meanQ == 0) {
                            options.getLog().println("    Couldn't get mean quality value");
                        } else {
                            if (meanQ >= options.getMinQ()) {
                                outputPF = NanoOKOptions.READTYPE_PASS;
                            } else {
                                outputPF = NanoOKOptions.READTYPE_FAIL;
                            }
                        }
                        options.getLog().println("    Mean quality " + meanQ + " output class " + (outputPF == NanoOKOptions.READTYPE_PASS ? "pass":"fail"));
                    }
                                        
                    String fastaqPathname = getFastaqFilename(fast5Pathname, t, inputPF, outputPF);
                    options.getLog().println("    Writing "+fastaqPathname);

                    options.getReadFileMerger().addReadFile(fastaqPathname, t, outputPF, ff.getID(), ff.getLength(), meanQ);

                    if (options.getReadFormat() == NanoOKOptions.FASTA) {
                        ff.writeFasta(fastaqPathname, options.outputFast5Path() ? fast5Pathname:null);
                    } else {
                        ff.writeFastq(fastaqPathname);
                    }

                    if (options.isBlastingRead()) {
                        addToBlast(fastaqPathname, t);
                    }

                    if (options.isAligningRead()) {
                        runAlign(fastaqPathname);
                    }
                }
            }
        }
    }

    private void runBlast(String fastaqPathname) {
        for (int t=0; t<3; t++) {
            if (options.isProcessingReadType(t)) {
                addToBlast(fastaqPathname, t);
            }
        }
    }
    
    public void run() {
        while (!fileWatcher.timedOut()) {
            FileWatcherItem fwi = null; 
            String fastaqPathname = null;
            String alignmentPathname = null;
            String parsedPathname = null;
            String alignmentLogPathname = null;
            
            // Get next file to process
            while ((fwi == null) && !fileWatcher.timedOut()) {
                fwi = fileWatcher.getPendingFile();
                if (fwi == null) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ReadProcessorRunnable.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            
            if (fwi != null) {
                String nextPathname = fwi.getPathname();
                int pf = fwi.getPassOrFail();

                // Check valid filename
                if (options.isExtractingReads()) {
                    if (nextPathname.toLowerCase().endsWith(".fast5")) {
                        runExtract(nextPathname, pf);
                    } else {
                        options.getLog().println("Invalid "+nextPathname);
                    }
                } else if (options.isConvertingFastQ()) {
                    if (nextPathname.toLowerCase().endsWith(".fastq")) {
                        runConvertFastQ(nextPathname);
                    }
                } else if (options.isAligningRead()) {
                    if (nextPathname.toLowerCase().endsWith(".fasta") || 
                        nextPathname.toLowerCase().endsWith(".fastq")) {
                        runAlign(nextPathname);
                    }                
                } else if (options.isParsingRead()) {
                    if (nextPathname.toLowerCase().endsWith(options.getParser().getAlignmentFileExtension())) {
                        alignmentPathname = nextPathname;
                        runParse(nextPathname);
                    }
                } else if (options.isBlastingRead()) {               
                    if (nextPathname.toLowerCase().endsWith(".fasta") || 
                        nextPathname.toLowerCase().endsWith(".fastq")) {
                        runBlast(nextPathname);
                    }                
                }
            }
        }
        
        options.getLog().println("Thread exiting");
    }
}
