public class CountdownLatch {
    private int count;

    public CountdownLatch(int count) {
        this.count=count;
    }

    public synchronized void countDown() {
        count--;
        if(count ==0) {
            notifyAll();
        }
    }

    public synchronized void await() throws InterruptedException {
        while(count != 0)
            wait();
        }
    }
