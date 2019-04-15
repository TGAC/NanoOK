/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nanook;

/**
 *
 * @author leggettr
 */
public class NedomeGenomeStats {
    private NanoOKOptions options;
    private int[] chromosomeLengths = {248956424,
                                       242193531,
                                       198295561,
                                       190214557,
                                       181538261,
                                       170805979,
                                       159345973,
                                       145138636,
                                       138394719,
                                       133797422,
                                       135086624,
                                       133275309,
                                       114364328,
                                       107043720,
                                       101991191,
                                       90338347,
                                       83257443,
                                       80373285,
                                       58617616,
                                       64444167,
                                       46709983,
                                       50818470,
                                       156040895,
                                       57227415};
    private int[] chromosomeYield = new int[24];
    private int[] chromosomeCounts = new int[24];
    private int assignedCount = 0;
    private int readsProcessedCount = 0;
    private long genomeSize = 0;

    public NedomeGenomeStats(NanoOKOptions o) {
        options = o;
                
        for (int i=0; i<24; i++) {
            chromosomeCounts[i] = 0;
            chromosomeYield[i] = 0;
            genomeSize += chromosomeLengths[i];
        }
    }
    
    public int idToNumber(String id) {
        String number = id.substring(7, 9);
        int n = Integer.parseInt(number) - 1;
        
        if ((n < 0) || (n>23)) {        
            System.out.println("Couldn't parse ID");
            n = -1;
        }

        return n;
    }
    
    public void addRead(String targetId, int size) {
        int n = idToNumber(targetId);
        //System.out.println(targetId + " = "+n);
        
        if (readsProcessedCount == 0) {
            readsProcessedCount = options.getReadsPerBlast();
        }
        
        if (n != -1) {
            chromosomeCounts[n]++;
            chromosomeYield[n] += size;
            assignedCount++;
        }
    }
    
    public int getChromosomeCount(int oneBasedChromosomeId) {
        return chromosomeCounts[oneBasedChromosomeId - 1];
    }

    public int getChromosomeYield(int oneBasedChromosomeId) {
        return chromosomeYield[oneBasedChromosomeId - 1];
    }    
    
    public int getTotalYield() {
        int total = 0;

        for (int i=0; i<24; i++) {
            total += chromosomeYield[i];
        }
                
        return total;
    }
    
    public double getTotalCoverage() {
        double totalYield = (double)getTotalYield();
        double gs = (double)genomeSize;
        double totalCoverage = totalYield / gs;
        
        System.out.println(totalYield + " / " + gs + " = "+ totalCoverage);

        return totalCoverage;
    }
     
    public double getChromosomeCoverage(int oneBasedChromosomeId) {
        return (double)chromosomeYield[oneBasedChromosomeId - 1] / (double)chromosomeLengths[oneBasedChromosomeId - 1];
    }

    public void addGenomeStats(NedomeGenomeStats ngs) {
        for (int i=1; i<=24; i++) {
            chromosomeCounts[i-1] += ngs.getChromosomeCount(i);
            chromosomeYield[i-1] += ngs.getChromosomeYield(i);
        }
        readsProcessedCount += ngs.getReadsProcessedCount();
        assignedCount += ngs.getAssigned();
        
    }
    
    public int getAssigned() {
        return assignedCount;
    }
    
    public int getUnassigned() {
        return readsProcessedCount - assignedCount;
    }
    
    public int getReadsProcessedCount() {
        return readsProcessedCount;
    }
}
