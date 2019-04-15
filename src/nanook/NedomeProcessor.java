/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nanook;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Hashtable;

/**
 *
 * @author leggettr
 */
public class NedomeProcessor implements Runnable {
    private NanoOKOptions options;
    private Hashtable<String,Integer> queryIds = new Hashtable<String,Integer>();
    private Hashtable<String,Integer> querySize = new Hashtable<String,Integer>();
    
    public NedomeProcessor(NanoOKOptions o) {
        options = o;
    }
    
    private void loadReadSizes(String pafFilename) {
        String sizesFilename = getFastaFilename(pafFilename) + ".sizes";
        
        System.out.println("Reading " + sizesFilename);
        try {
            BufferedReader br = new BufferedReader(new FileReader(sizesFilename));
            String line;                

            while ((line = br.readLine()) != null) {
                String[] fields = line.split("\\t");
                String id = fields[0];
                int length = Integer.parseInt(fields[1]);
                querySize.put(id, length);
                //System.out.println("Read ["+id+"]");
            }
            br.close();        
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void processPafFile(NedomeGenomeStats genomeStats, String filename) {
        NedomeGenomeStats ngs = new NedomeGenomeStats(options);
        System.out.println("Processing "+filename);
        
        loadReadSizes(filename);
        
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            String line;                

            while ((line = br.readLine()) != null) {
                String[] fields = line.split("\\t");
                String queryName = fields[0];
                String targetName = fields[5];
                
                if (!queryIds.containsKey(queryName)) {
                    int size = 1000;
                    if (querySize.containsKey(queryName)) {
                       size = querySize.get(queryName);
                    } else {
                        System.out.println("Can't find ["+queryName+"]");
                    }
                    queryIds.put(queryName, 1);
                    ngs.addRead(targetName, size);
                }
                
            }
            br.close();        
            genomeStats.addGenomeStats(ngs);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public String getFastaFilename(String pafFilename) {
        File f = new File(pafFilename);
        String prefix = f.getName().substring(0, f.getName().indexOf("_minimap"));
        String mergedPathname = options.getReadDir() + 
                                "_chunks" + File.separator +
                                prefix + 
                                (options.getReadFormat() == NanoOKOptions.FASTA ? ".fasta":".fastq");
        
        System.out.println("Merged pathname "+mergedPathname);

        return mergedPathname;
    }
    
    @Override
    public void run() {
        NedomeGenomeStats genomeStats = new NedomeGenomeStats(options);
        
        System.out.println("Processing nedome chunks");
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String minimapDir = options.getSampleDirectory() + File.separator + "minimap_human";
        File dir = new File(minimapDir);
        
        File[] listOfFiles = dir.listFiles();

        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (!file.isDirectory()) {
                    if (file.getName().endsWith(".completed")) {
                        String pafFilename = file.getPath().substring(0, file.getPath().length() - 10);
                        
                        processPafFile(genomeStats, pafFilename);
                    }
                }
            }           
        }       
        
        //for (int i=1; i<=24; i++) {
        //    System.out.println("Chr"+i+"\t"+genomeStats.getChromosomeCount(i));
        //}
        //System.out.println("Ass\t"+genomeStats.getAssigned());
        //System.out.println("Unass\t"+genomeStats.getUnassigned());
        //System.out.println("Total\t"+genomeStats.getReadsProcessedCount());
        
        try {
            System.out.println("Writing "+options.getNedomeFile());
            PrintWriter pw = new PrintWriter(new FileWriter(options.getNedomeFile())); 
            
            pw.print("{");
            int totalCount = 0;
            for (int i=1; i<=24; i++) {
                pw.print("\"chr"+i+"\"");
                pw.print(":{\"coverage\":" + genomeStats.getChromosomeCoverage(i));
                pw.print(",\"read_count\":" + genomeStats.getChromosomeCount(i));
                pw.println("},");
                totalCount+=genomeStats.getChromosomeCount(i);
            }

            pw.print("\"nedome\":{\"coverage\":" + genomeStats.getTotalCoverage());
            pw.println(",\"read_count\":" + genomeStats.getReadsProcessedCount() + "}}");
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
                
        options.setNedProcessorRunning(false);
    }
    
}
