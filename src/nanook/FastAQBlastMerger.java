package nanook;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class FastAQBlastMerger implements Runnable {
    private NanoOKOptions options;
    private ArrayList<String> listOfFiles;
    private String mergedFilePrefix;
    private int fileCounter;
    private String defaultFormatString = "6 qseqid sseqid pident length mismatch gapopen qstart qend sstart send evalue bitscore stitle staxid";

    public FastAQBlastMerger(NanoOKOptions o, String m, ArrayList a, int fc) {
        options = o;
        mergedFilePrefix = m;
        listOfFiles = a;
        fileCounter = fc;        
    }

    private void runBlastBacteria() {
        File iff = new File(mergedFilePrefix);
        String inputFasta = mergedFilePrefix + "_" + fileCounter + ".fasta";
        String outputBlast = options.getSampleDirectory() + File.separator + "blastn_bacteria" + File.separator + iff.getName() + "_" + fileCounter + "_blast_bacteria.txt";
        String commandFile = options.getSampleDirectory() + File.separator + "blastn_bacteria" + File.separator + iff.getName() + "_" + fileCounter + "_blast_bacteria.sh";
        String logFile = options.getSampleDirectory() + File.separator + "blastn_bacteria" + File.separator + iff.getName() + "_" + fileCounter + "_blast_bacteria.log";
        String formatString = "'" + defaultFormatString + "'";
        
        try {
            SimpleJobScheduler jobScheduler = options.getJobScheduler();
            System.out.println("Writing blast command file "+commandFile);
            PrintWriter pw = new PrintWriter(new FileWriter(commandFile));
            String command = "blastn -db "+options.getBacteriaPath()+" -query " + inputFasta + " -evalue 0.001 -show_gis -out " + outputBlast + " -outfmt "+formatString;
            //String command = "blastn -db " + options.getBacteriaPath()+" -query " + inputFasta + " -evalue " + Double.toString(options.getBlastMaxE()) + " -max_target_seqs " + Integer.toString(options.getBlastMaxTargetSeqs()) + " -show_gis -out " + outputBlast + " -outfmt "+formatString;
            pw.write(command);
            pw.close();
            
            if (jobScheduler == null) {            
                options.getLog().println("Submitting blast command file to SLURM "+commandFile);
                ProcessLogger pl = new ProcessLogger();
                String[] commands = {"slurmit",
                                     "-o", logFile,
                                     "-p", "Nanopore",
                                     "-m", "8G",
                                     "sh "+commandFile};
                pl.runCommandToLog(commands, options.getLog());
            } else {
                String[] commands = {"blastn",
                                     "-db",
                                     options.getBacteriaPath(),
                                     "-query", inputFasta,
                                     "-evalue", Double.toString(options.getBlastMaxE()),
                                     "-max_target_seqs", Integer.toString(options.getBlastMaxTargetSeqs()),
                                     "-show_gis",
                                     "-task", "megablast",
                                     "-out", outputBlast,
                                     "-outfmt", formatString};
                System.out.println("Submitting from FastAQBlastMerger");
                jobScheduler.submitJob(commands, logFile);
            }
        } catch (IOException e) {
            System.out.println("runBlast exception");
            e.printStackTrace();
        }
    }    
    
    private void runBlastnt() {
        File iff = new File(mergedFilePrefix);
        String inputFasta = mergedFilePrefix + "_" + fileCounter + ".fasta";
        String outputBlast = options.getSampleDirectory() + File.separator + "blastn_nt" + File.separator + iff.getName() + "_" + fileCounter + "_blast_nt.txt";
        String commandFile = options.getSampleDirectory() + File.separator + "blastn_nt" + File.separator + iff.getName() + "_" + fileCounter + "_blast_nt.sh";
        String logFile = options.getSampleDirectory() + File.separator + "blastn_nt" + File.separator + iff.getName() + "_" + fileCounter + "_blast_nt.log";
        String formatString = "'" + defaultFormatString + "'";

        try {
            System.out.println("Writing blast command file "+commandFile);
            PrintWriter pw = new PrintWriter(new FileWriter(commandFile));
            pw.write("blastn -db "+options.getntPath()+" -query " + inputFasta + " -evalue 0.001 -show_gis -out " + outputBlast + " -outfmt "+formatString);
            pw.close();
            
            options.getLog().println("Submitting blast command file to SLURM "+commandFile);
            ProcessLogger pl = new ProcessLogger();
            String[] commands = {"slurmit",
                                 "-o", logFile,
                                 "-p", "tgac-medium",
                                 "-m", "16G",
                                 "sh "+commandFile};
            pl.runCommandToLog(commands, options.getLog());
        } catch (IOException e) {
            System.out.println("runBlast exception");
            e.printStackTrace();
        }
    }

    private void runBlastCard() {
        File iff = new File(mergedFilePrefix);
        String inputFasta = mergedFilePrefix + "_" + fileCounter + ".fasta";
        String outputBlast = options.getSampleDirectory() + File.separator + "blastn_card" + File.separator + iff.getName() + "_" + fileCounter + "_blast_card.txt";
        String commandFile = options.getSampleDirectory() + File.separator + "blastn_card" + File.separator + iff.getName() + "_" + fileCounter + "_blast_card.sh";
        String logFile = options.getSampleDirectory() + File.separator + "blastn_card" + File.separator + iff.getName() + "_" + fileCounter + "_blast_card.log";
        String formatString = "'" + defaultFormatString + "'";

        try {
            System.out.println("Writing blast command file "+commandFile);
            PrintWriter pw = new PrintWriter(new FileWriter(commandFile));
            pw.write("blastn -db "+options.getCardPath()+" -query " + inputFasta + " -evalue 0.001 -show_gis -out " + outputBlast + " -outfmt "+formatString);
            pw.close();
            
            options.getLog().println("Submitting blast command file to SLURM "+commandFile);
            ProcessLogger pl = new ProcessLogger();
            String[] commands = {"slurmit",
                                 "-o", logFile,
                                 "-p", "Nanopore",
                                 "-m", "4G",
                                 "sh "+commandFile};
            pl.runCommandToLog(commands, options.getLog());            
        } catch (IOException e) {
            System.out.println("runBlast exception");
            e.printStackTrace();
        }
    }  
    
    private void mergeFiles() {
        String mergedFile = mergedFilePrefix + "_" + fileCounter + ".fasta";

        options.getLog().println("Writing merged file "+mergedFile);
        
        try {
            PrintWriter pw = new PrintWriter(new FileWriter(mergedFile));

            for (int i=0; i<listOfFiles.size(); i++) {
                BufferedReader br = new BufferedReader(new FileReader(listOfFiles.get(i)));
                String line;                
                while ((line = br.readLine()) != null) {
                    pw.println(line);
                }
                br.close();
            }
            pw.close();
        } catch (IOException e) {
            System.out.println("mergeFiles exception");
            e.printStackTrace();
        }
        
    }    
    
    public void run() {
        mergeFiles();
        runBlastBacteria();
        runBlastCard();
        runBlastnt();        
    }
}
