package com.intel.galileo.flash.tool;

public class ProcessManager implements Runnable {
    Process process;
    private boolean finished;

    public ProcessManager(Process process) {
        this.process = process;
    }

    public void run() {
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            // Ignore
        }
        synchronized (this) {
            notifyAll();
            finished = true;
        }
    }

    public synchronized void waitForOrKill(long millis) {
        if (!finished) {
            try {
                wait(millis);
            } catch (InterruptedException e) {
                // Ignore
            }
            if (!finished) {
                process.destroy();
                System.out.println("Process timed out");
            }
        }
    }
}
