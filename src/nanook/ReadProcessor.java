/*
 * Program: NanoOK
 * Author:  Richard M. Leggett (richard.leggett@earlham.ac.uk)
 * 
 * Copyright 2015-17 Earlham Institute
 */

package nanook;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Read extractor
 * 
 * @author Richard Leggett
 */
public class ReadProcessor {
    private NanoOKOptions options;
    private ThreadPoolExecutor executor;
    private long lastCompleted = -1;
    FileWatcher fw = null;

    /**
     * Constructor
     * @param o program options
     */
    public ReadProcessor(NanoOKOptions o) {    
        options = o;
        fw = new FileWatcher(options);

        //executor = Executors.newFixedThreadPool(options.getNumberOfThreads());
        executor = new ThreadPoolExecutor(options.getNumberOfThreads(), options.getNumberOfThreads(), 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    }
    
    /**
     * Write progress of extraction
     */
    private void writeProgress() {
        long completed = executor.getCompletedTaskCount();
        long total = executor.getTaskCount();
        long e = 0;
        long s = NanoOKOptions.PROGRESS_WIDTH;
        
        if (total > 0) {
            e = NanoOKOptions.PROGRESS_WIDTH * completed / total;
            s = NanoOKOptions.PROGRESS_WIDTH - e;
        }
        
        if (completed != lastCompleted) {              
            System.out.print("\rExtraction [");
            for (int i=0; i<e; i++) {
                System.out.print("=");
            }
            for (int i=0; i<s; i++) {
                System.out.print(" ");
            }
            System.out.print("] " + completed +"/" +  total);
            lastCompleted = completed;
        }
    }
    
    /**
     * Process a directory and extract reads
     * @param inputDirName input directory name
     * @param outputDirName output directory name
     */
    private void processDirectory(String inputDirName, boolean allowSubdir, boolean processThisDir, int pf) {        
        options.getLog().println("Processing directory");
        options.getLog().println("Input dir name: "+inputDirName);
        options.getLog().println("allowSubdir: "+allowSubdir);        
        options.getLog().println("processThisDir: "+processThisDir);
                        
        if (processThisDir) {            
            if (options.usingBatchDirs()) {
                fw.addBatchContainer(inputDirName, pf);
            } else {
                fw.addWatchDir(inputDirName, pf);
            }
        } else {
            File inputDir = new File(inputDirName);
            File[] listOfFiles = inputDir.listFiles();

            if (listOfFiles == null) {
                options.getLog().println("Directory "+inputDirName+" doesn't exist");
            } else if (listOfFiles.length <= 0) {
                options.getLog().println("Directory "+inputDirName+" empty");
            } else {
                for (File file : listOfFiles) {
                    if (file.isDirectory() && allowSubdir) {
                        processDirectory(inputDirName + File.separator + file.getName(),
                                         false,
                                         true,
                                         pf);
                    }
                }           
            }
        }    
    }
    
    private void addDirsForExtract() {
        if (options.usingPassFailDirs()) {
            if (options.isProcessingPassReads()) {
                processDirectory(options.getFast5Dir() + File.separator + "pass",
                                 options.isBarcoded(),
                                 options.isBarcoded() ? false:true,
                                 NanoOKOptions.READTYPE_PASS);
            }
            
            if (options.isProcessingFailReads()) {
                processDirectory(options.getFast5Dir() + File.separator + "fail",
                                 options.isBarcoded(),
                                 true,
                                 NanoOKOptions.READTYPE_FAIL);
            }
        } else {
            processDirectory(options.getFast5Dir(),
                             options.isBarcoded(),
                             options.isBarcoded() ? false:true,
                             NanoOKOptions.READTYPE_COMBINED);
        }        
    }

    private void addDirsForAlign() {
        // If using batch dirs, then we go sample/fasta/2D/pass/batch_XXX
        //                              or sample/fasta/2D/pass/barcodeXXX/batch_XXX
        // If using old style, then we go sample/fasta/pass/2D
        //                             or sample/fasta/pass/2D/barcodeXXX/barcodeXXX

        options.getLog().println("Adding directories");
        options.getLog().println("usingPassFailDirs: " + options.usingPassFailDirs());
        options.getLog().println("isProcessingPassReads: " + options.isProcessingPassReads());
        options.getLog().println("isProcessingFailReads: " + options.isProcessingFailReads());
        
        for (int t=0; t<3; t++) {   
            if (options.isProcessingReadType(t)) {
                if (options.usingPassFailDirs()) {
                    if (options.isProcessingPassReads()) {
                        //if (options.usingBatchDirs()) {
                            processDirectory(options.getReadDir() + File.separator + "pass" + File.separator + NanoOKOptions.getTypeFromInt(t),
                                             options.isBarcoded(),
                                             options.isBarcoded() ? false:true,
                                             NanoOKOptions.READTYPE_PASS);                        
                        //} else {
                        //    processDirectory(options.getReadDir() + File.separator + "pass" + File.separator + NanoOKOptions.getTypeFromInt(t),
                        //                     options.isBarcoded(),
                        //                     options.isBarcoded() ? false:true);
                        //}
                    }

                    if (options.isProcessingFailReads()) {
                        //if (options.usingBatchDirs()) {
                            processDirectory(options.getReadDir() + File.separator + "fail" + File.separator + NanoOKOptions.getTypeFromInt(t),
                                             options.isBarcoded(),
                                             options.isBarcoded() ? false:true,
                                             NanoOKOptions.READTYPE_FAIL);                            
                        //} else {
                        //    processDirectory(options.getReadDir() + File.separator + "fail" + File.separator + NanoOKOptions.getTypeFromInt(t),
                        //                     options.isBarcoded(),
                        //                     true);                            
                        //}
                    }
                } else {
                    processDirectory(options.getReadDir() + File.separator + NanoOKOptions.getTypeFromInt(t),
                                     options.isBarcoded(),
                                     options.isBarcoded() ? false:true,
                                     NanoOKOptions.READTYPE_COMBINED);
                }        
            }
        }
    }
    
    private void addDirsForConvertFastQ() {
        String fastQConvertDir = options.getFastQConvertDir();
        String processDir;
        
        if ((fastQConvertDir.startsWith("~/")) || (fastQConvertDir.startsWith("/"))) {
            processDir = fastQConvertDir;
        } else {
            processDir = options.getSampleDirectory() + "/" + fastQConvertDir;
        }
        
        System.out.println("FASTQ convert dir: " + processDir);
        
        processDirectory(processDir, false, true, NanoOKOptions.READTYPE_PASS);
    }

    private void addDirsForParse() {
        // If using batch dirs, then we go sample/last/2D/pass/batch_XXX
        // If using old style, then we go sample/last/pass/2D
        for (int t=0; t<3; t++) {   
            if (options.isProcessingReadType(t)) {
                if (options.usingPassFailDirs()) {
                    if (options.isProcessingPassReads()) {
                        //if (options.usingBatchDirs()) {
                            processDirectory(options.getAlignerDir() + File.separator + "pass" + File.separator + NanoOKOptions.getTypeFromInt(t),
                                             options.isBarcoded(),
                                             options.isBarcoded() ? false:true,
                                             NanoOKOptions.READTYPE_PASS);
                        //} else {
                        //    processDirectory(options.getAlignerDir() + File.separator + "pass" + File.separator + NanoOKOptions.getTypeFromInt(t),
                        //                     options.isBarcoded(),
                        //                     options.isBarcoded() ? false:true);
                        //}
                    }

                    if (options.isProcessingFailReads()) {
                        //if (options.usingBatchDirs()) {
                            processDirectory(options.getAlignerDir() + File.separator + "pass" + File.separator + NanoOKOptions.getTypeFromInt(t),
                                             options.isBarcoded(),
                                             options.isBarcoded() ? false:true,
                                             NanoOKOptions.READTYPE_FAIL);                            
                        //} else {
                        //    processDirectory(options.getAlignerDir() + File.separator + "fail" + File.separator + NanoOKOptions.getTypeFromInt(t),
                        //                     options.isBarcoded(),
                        //                     true);
                        //}
                    }
                } else {
                    processDirectory(options.getAlignerDir() + File.separator + NanoOKOptions.getTypeFromInt(t),
                                     options.isBarcoded(),
                                     options.isBarcoded() ? false:true,
                                     NanoOKOptions.READTYPE_COMBINED);
                }        
            }
        }
    }
    
    /**
     * Extract reads
     */
    public void process() throws InterruptedException {      
        String baseDir = "";
        
        options.getLog().println("extractingReads: "+options.isExtractingReads());
        options.getLog().println("convertingFastQ: "+options.isConvertingFastQ());
        options.getLog().println("aligningReads: "+options.isAligningRead());
        options.getLog().println("parsingReads: "+options.isParsingRead());
        options.getLog().println("blastingReads: "+options.isBlastingRead());
        
        if (options.isExtractingReads()) {
            options.getSampleChecker().checkFast5Directory();
            addDirsForExtract();
        } else if (options.isConvertingFastQ()) {
            addDirsForConvertFastQ();
        } else if (options.isAligningRead()) {
            options.getSampleChecker().checkReadDirectory();
            addDirsForAlign();
        } else if (options.isParsingRead()) {
            options.getSampleChecker().checkReadDirectory();
            addDirsForParse();
        } else if (options.isBlastingRead()) {
            options.getSampleChecker().checkReadDirectory();
            addDirsForAlign();
        }
        
        
        for (int i=0; i<options.getNumberOfThreads(); i++) {
            executor.execute(new ReadProcessorRunnable(options, fw));
        }
        
        // Now keep scanning
        while (!fw.timedOut()) {
            if (options.getJobScheduler() != null) {
                options.getJobScheduler().manageQueue();
            }
            fw.scan();
            fw.writeProgress();
            Thread.sleep(500);            
            fw.writeProgress();
            Thread.sleep(500);    
        }
        fw.writeProgress();
                
        // That's all - wait for all threads to finish
        executor.shutdown();

        options.getReadFileMerger().closeFiles();
        if (options.mergeFastaFiles()) {        
            System.out.println("");
            options.getReadFileMerger().writeMergedFiles();
        }        
        
        //writeProgress();
        System.out.println("");
        System.out.println("");
        System.out.println("DONE");
    }  
}


