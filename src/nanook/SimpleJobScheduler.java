
package nanook;

import java.io.File;
import java.util.LinkedList;

public class SimpleJobScheduler {
    private LinkedList<SimpleJobSchedulerJob> pendingJobs = new LinkedList<SimpleJobSchedulerJob>();
    private LinkedList<SimpleJobSchedulerJob> runningJobs = new LinkedList<SimpleJobSchedulerJob>();
    private LinkedList<SimpleJobSchedulerJob> finishedJobs = new LinkedList<SimpleJobSchedulerJob>();
    private NanoOKLog schedulerLog = new NanoOKLog();
    private NanoOKOptions options;
    private int maxJobs = 4;
    private int jobId = 0;
    
    public SimpleJobScheduler(int m, NanoOKOptions o) {
        maxJobs = m;
        options = o;
        schedulerLog.open(o.getLogsDir()+File.separator+"scheduler.txt");
        if (options.isNedome()) {
            maxJobs = 1;
        }
    }
    
    public int submitJob(String[] commands, String logFilename) {
        SimpleJobSchedulerJob j = new SimpleJobSchedulerJob(jobId, commands, logFilename);
        pendingJobs.add(j);
        schedulerLog.println("Submitted job\t"+jobId+"\t"+j.getCommand());
        return jobId++;
    }

    public int submitJob(String[] commands, String logFilename, String errorFilename) {
        SimpleJobSchedulerJob j = new SimpleJobSchedulerJob(jobId, commands, logFilename, errorFilename);
        pendingJobs.add(j);
        schedulerLog.println("Submitted job\t"+jobId+"\t"+j.getCommand());
        return jobId++;
    }
    
    public void manageQueue() {
        // Check for any finished jobs
        for (int i=0; i<runningJobs.size(); i++) {
            SimpleJobSchedulerJob j = runningJobs.get(i);
            if (j.hasFinished()) {
                schedulerLog.println("Finished job\t" +j.getId() + "\t" + j.getCommand());
                
                if (options.isNedome()) {
                    String completedFile = j.getLog() + ".completed";
                    File f = new File(completedFile);
                    try {
                        f.createNewFile();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    
                    options.updateNedome();
                }
                
                runningJobs.remove(i);
                finishedJobs.add(j);
            }
        }
        
        // Now can we move jobs from pending to running?
        while ((runningJobs.size() < maxJobs) && (pendingJobs.size() > 0)) {
            SimpleJobSchedulerJob j = pendingJobs.remove();
            schedulerLog.println("Running job\t" + j.getId() + "\t" +j.getCommand());
            //schedulerLog.println("Logging "+j.getLog());
            runningJobs.add(j);
            j.run();
        }
    }    
}
