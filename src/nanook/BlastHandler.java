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

public class BlastHandler {
    private NanoOKOptions options = null;
    private int type;
    private int passfail;
    private int nSeqs = 0;
    private int fileCounter = 0;
    private ArrayList<String> mergeList = new ArrayList<String>();
    private String defaultFormatString = "6 qseqid sseqid pident length mismatch gapopen qstart qend sstart send evalue bitscore stitle staxids";
    
    public BlastHandler(NanoOKOptions o, int t, int pf) {
        options = o;
        type = t;
        passfail = pf;
        if (options.getFileCounterOffset() > 0) {
            fileCounter = options.getFileCounterOffset();
            System.out.println("File offset "+fileCounter);
        }        
    }
    
    private void writeMeganFile() {
        ArrayList<String> blastProcesses = options.getBlastProcesses();
        String meganDir = options.getSampleDirectory() + File.separator + "megan";
        File f = new File(meganDir);
        
        if (!f.exists()) {
            f.mkdir();
        }
        
        for (int i=0; i<blastProcesses.size(); i++) {
            String[] params = blastProcesses.get(i).split(",");
            if (params.length == 5) {
                String blastName = params[0];
                String blastTool = params[1];
                String blastDb = params[2];
                String memory = params[3];
                String queue = params[4];
                String cmdPathname = meganDir + File.separator + "all_" + NanoOKOptions.getTypeFromInt(type) +
                                    "_" + NanoOKOptions.getPassFailFromInt(passfail) + "_" + Integer.toString(fileCounter) + ".cmds";
                String meganPathname = meganDir + File.separator + "all_" + NanoOKOptions.getTypeFromInt(type) +
                                    "_" + NanoOKOptions.getPassFailFromInt(passfail) + "_" + Integer.toString(fileCounter) + ".rma";
                String slurmPathname = meganDir + File.separator + "all_" + NanoOKOptions.getTypeFromInt(type) +
                                    "_" + NanoOKOptions.getPassFailFromInt(passfail) + "_" + Integer.toString(fileCounter) + ".slurm.sh";
                String slurmLogname = meganDir + File.separator + "all_" + NanoOKOptions.getTypeFromInt(type) +
                                    "_" + NanoOKOptions.getPassFailFromInt(passfail) + "_" + Integer.toString(fileCounter) + ".slurm.log";
                
                try {
                    options.getLog().println("Writing MEGAN command file " + cmdPathname);
                    PrintWriter pw = new PrintWriter(new FileWriter(cmdPathname));
                    pw.println("setprop MaxNumberCores=4;");
                    String blastFileString="";
                    String fastaFileString="";
                    
                    for (int fc=0; fc<=fileCounter; fc++) {
                        String fileName = "all_" + NanoOKOptions.getTypeFromInt(type) + "_" + NanoOKOptions.getPassFailFromInt(passfail) + "_" +  Integer.toString(fc);
                        String fastaPathname = options.getReadDir() + "_chunks" + File.separator + fileName + (options.getReadFormat() == NanoOKOptions.FASTA ? ".fasta":".fastq");
                        String blastPathname = options.getSampleDirectory() + File.separator +
                                                 blastTool + "_" + blastName + File.separator + 
                                                 fileName + "_" + blastTool + "_" + blastName + ".txt";
                        if (blastFileString != "") {
                            blastFileString += ",";
                            fastaFileString += ",";
                        }
                        fastaFileString = fastaFileString + "'" + fastaPathname + "'";
                        blastFileString = blastFileString + "'" + blastPathname + "'";
                    }
                    
                    pw.print("import blastFile="+blastFileString+" fastaFile="+fastaFileString +" meganFile="+meganPathname);
                    pw.println(" maxMatches=100 maxExpected=0.001 minSupport=1 minComplexity=0 blastFormat=BlastTAB;");
                    pw.println("quit;");
                    pw.close();
                    
                    pw = new PrintWriter(new FileWriter(slurmPathname));
                    if (!options.isMac()) {
                        pw.print("slurmit -p ei-medium -c 4 -o " + slurmLogname + " -m \"8G\" \"");
                    }
                    pw.print(options.getMeganCmdLine());
                    pw.println(" -g -c " + cmdPathname + " -L " + options.getMeganLicense());
                    
                    if (!options.isMac()) {
                        pw.print("\"");
                    }
                    
                    pw.close();
                } catch (Exception e) {
                    System.out.println("writeMeganFile exception");
                    e.printStackTrace();
                }
            }
        }
    }
    
    private void runBlasts(String inputPathname) {
        String formatString = "'" + defaultFormatString + "'";
        ArrayList<String> blastProcesses = options.getBlastProcesses();
        File iff = new File(inputPathname);
        String fileName = iff.getName();
        String filePrefix = fileName;
        
        if (filePrefix.contains(".")) {
            filePrefix = fileName.substring(0, fileName.lastIndexOf('.'));
        }
        
        for (int i=0; i<blastProcesses.size(); i++) {
            String[] params = blastProcesses.get(i).split(",");
            if (params.length == 5) {
                String blastName = params[0];
                String blastTool = params[1];
                String blastDb = params[2];
                String memory = params[3];
                String queue = params[4];
                String outputBlast = options.getSampleDirectory() + File.separator +
                                     blastTool + "_" + blastName + File.separator + 
                                     filePrefix + "_" + blastTool + "_" + blastName + ".txt";
                String commandFile = options.getSampleDirectory() + File.separator +
                                     blastTool + "_" + blastName + File.separator +
                                     filePrefix + "_" + blastTool + "_" + blastName + ".sh";
                String logFile = options.getLogsDir() + File.separator +
                                 blastTool + "_" + blastName + File.separator +
                                 filePrefix + "_" + blastTool + "_" + blastName + ".log";

                options.getLog().println("  BLAST input: " + inputPathname);
                options.getLog().println(" BLAST output: " + outputBlast);
                options.getLog().println("BLAST command: " + commandFile);
                options.getLog().println("    BLAST log: " + logFile);
                
                try {
                    options.getLog().println("Writing blast command file "+commandFile);
                    PrintWriter pw = new PrintWriter(new FileWriter(commandFile));
                    // TODO: -task option shouldn't be hardcoded
                    String command = "";
                    SimpleJobScheduler jobScheduler = options.getJobScheduler();
                    
                    if (options.isNedome()) {
                        command = blastTool + " -db " + blastDb + " -query " + inputPathname + " -evalue " + Double.toString(options.getBlastMaxE()) + " -max_target_seqs 1 -max_hsps 1 -task megablast -out " + outputBlast + " -outfmt "+formatString;
                    } else {
                        command = blastTool + " -db " + blastDb + " -query " + inputPathname + " -evalue " + Double.toString(options.getBlastMaxE()) + " -max_target_seqs " + Integer.toString(options.getBlastMaxTargetSeqs()) + " -show_gis -task megablast -out " + outputBlast + " -outfmt "+formatString;
                    }
                    pw.write(command);
                    pw.close();

                    if (jobScheduler == null) {
                        options.getLog().println("Submitting blast command file to SLURM "+commandFile);
                        ProcessLogger pl = new ProcessLogger();
                        String[] commands = {"slurmit",
                                             "-o", logFile,
                                             "-p", queue,
                                             "-m", memory,
                                             "sh "+commandFile};
                        pl.runCommandToLog(commands, options.getLog());            
                    } else if (options.isNedome()) {
//                        String[] commands = {blastTool,
//                                             "-db", blastDb,
//                                             "-query", inputPathname,
//                                             "-evalue", Double.toString(options.getBlastMaxE()),
//                                             "-max_target_seqs", "1",
//                                             "-max_hsps", "1",
//                                             "-task", "megablast",
//                                             "-out", outputBlast,
//                                             "-outfmt", defaultFormatString};

                        String outputMinimap = options.getSampleDirectory() + File.separator +
                                     blastTool + "_" + blastName + File.separator + 
                                     filePrefix + "_" + blastTool + "_" + blastName + ".paf";

                        String[] commands = {"minimap2",
                                             "-x", "map-ont",
                                             blastDb,
                                             inputPathname};

                        File f = new File(outputMinimap);
                        if (f.exists()) {
                            f.delete();
                            System.out.println("Deleted existing file "+f.getPath());
                        }
                        
                        f = new File(outputMinimap + ".completed");
                        if (f.exists()) {
                            f.delete();
                            System.out.println("Deleted existing file "+f.getPath());
                        }
                        
                        jobScheduler.submitJob(commands, outputMinimap, logFile);
                        System.out.println("Submitting from BlastHandler");                        
                    } else {
                        String[] commands = {blastTool,
                                             "-db", blastDb,
                                             "-query", inputPathname,
                                             "-evalue", Double.toString(options.getBlastMaxE()),
                                             "-max_target_seqs", Integer.toString(options.getBlastMaxTargetSeqs()),
                                             "-show_gis",
                                             "-task", "megablast",
                                             "-out", outputBlast,
                                             "-outfmt", defaultFormatString};
                                                
                        jobScheduler.submitJob(commands, logFile);
                        System.out.println("Submitting from BlastHandler");
                    }
                } catch (IOException e) {
                    System.out.println("runBlast exception");
                    e.printStackTrace();
                }
            } else {
                System.out.println("Badly formatted BLAST process: "+blastProcesses.get(i));
            }
        }
    }    
    
    private String mergeInputFiles() {
        String mergedPathname = options.getReadDir() + 
                                "_chunks" + File.separator + 
                                "all_" + NanoOKOptions.getTypeFromInt(type) + "_" + NanoOKOptions.getPassFailFromInt(passfail) + "_" + 
                                Integer.toString(fileCounter) + 
                                (options.getReadFormat() == NanoOKOptions.FASTA ? ".fasta":".fastq");

        options.getLog().println("Writing merged file "+mergedPathname);
               
        try {
            PrintWriter pw = new PrintWriter(new FileWriter(mergedPathname));
            PrintWriter pwSizes = null;

            if (options.isNedome()) {
                pwSizes = new PrintWriter(new FileWriter(mergedPathname + ".sizes"));
            }
            
            for (int i=0; i<mergeList.size(); i++) {                
                BufferedReader br = new BufferedReader(new FileReader(mergeList.get(i)));
                String line;
                String id = "Unknown";
                int lineCount = 0;
                int readSize = 0;

                while ((line = br.readLine()) != null) {
                    pw.println(line);
                    
                    if (options.isNedome()) {
                        lineCount++;
                        if (lineCount == 1) {
                            id = line.substring(1, line.indexOf(' '));
                        } else {
                            readSize += line.length();
                        }
                    }

                }
                br.close();

                if (options.isNedome()) {
                    pwSizes.println(id + "\t" + readSize);
                }
            }
            pw.close();
            if (options.isNedome()) {
                pwSizes.close();
            }
        } catch (IOException e) {
            System.out.println("mergeFiles exception");
            e.printStackTrace();
        }
        return mergedPathname;
    }     
    
    public synchronized void addRead(String readFilename) {
        mergeList.add(readFilename);
        nSeqs++;
        if (nSeqs == options.getReadsPerBlast()) {
            options.getLog().println("Merging files (nSeqs = "+nSeqs+")");
            String mergedPathname = mergeInputFiles();
            runBlasts(mergedPathname);
            writeMeganFile();
            
            //options.getThreadExecutor().execute(new FastAQMerger(options, mergedFilename, mergeList, fileCounter));
            mergeList = new ArrayList(); 
            fileCounter++;
            nSeqs = 0;
        }
    }    
    
    public synchronized void addReadChunk(String readFilename) {
        runBlasts(readFilename);
        //writeMeganFile();
    }
}
