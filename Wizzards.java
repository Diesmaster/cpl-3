import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;


class Monitor {
	private int numOfWizzards;
	final Lock lock;
	private enum States {wantToCast, waiting, casting};
	private States [] state;
	final Condition [] cond;

	Monitor(int numOfWizz){
		this.numOfWizzards = numOfWizz;
		lock = new ReentrantLock();
		state = new States[numOfWizzards];
		cond = new Condition[numOfWizzards];

		for(int i = 0; i < numOfWizzards; i++){
			state[i] = States.waiting;
			cond[i] = lock.newCondition();
		}
	}

	public void trytocast(int i){
		lock.lock();
		try{

			state[i] = States.wantToCast;

			if( ( state[(i-1+numOfWizzards)%numOfWizzards] != States.casting ) &&
			    (state[(i+1)%numOfWizzards] != States.casting) ){
				System.out.format("Wizzards %d picks up gem\n", i+1);
				System.out.format("Wizzards %d picks up want\n", i+1);
				state[i] = States.casting;
			}
			else {
				try {
					cond[i].await();

					System.out.format("Wizzards %d picks up gem\n", i+1);
					System.out.format("Wizzards %d picks up want\n", i+1);
					state[i] = States.casting;
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
			}
		}
		finally{
			lock.unlock();
		}
	}

	public void stopcasting(int i){
		lock.lock();
		try{
			System.out.format("Wizzards %d puts down gem\n", i+1);
			System.out.format("Wizzards %d puts down  want\n", i+1);
			state[i] = States.waiting;

			int left = (i - 1 + numOfWizzards)%numOfWizzards;
			int left2 = (i - 2 + numOfWizzards)%numOfWizzards;
			if( (state[left] == States.wantToCast) &&
				(state[left2] != States.casting) ){
				cond[left].signal();
			}

			if( (state[(i+1)%numOfWizzards] == States.wantToCast) &&
				(state[(i+2)%numOfWizzards] != States.casting) ){
				cond[(i+1)%numOfWizzards].signal();
			}
		}
		finally {
			lock.unlock();
		}
	}
}


public class Wizzards implements Runnable {

	private int myId;
	private int apprenticies;
	private Monitor mon;
	private Thread t;
	private int sleepLength;

	Wizzards(int id, int numToCast, Monitor m){
		this.myId = id;
		this.apprenticies = numToCast;
		this.mon = m;
		sleepLength = 10;
		t = new Thread(this);
		t.start();
	}

	@Override
	public void run() {
		int count = 1;
		while(count <= apprenticies ){
			mon.trytocast(myId);
			cast(count);
			mon.stopcasting(myId);
			++count;
		}
	}

	void cast(int count){
		System.out.format("Wizzards %d casts (%d times)\n", myId+1, count);
		try {
		    Thread.sleep(sleepLength);
		}
		catch (InterruptedException e) {}
	}

	public static void main(String[] args) {
		int numOfWizzards = 5;
		int apprenticies = 3;

		Monitor mon = new Monitor(numOfWizzards);
		Wizzards [] p = new Wizzards[numOfWizzards];

		System.out.println("the council is starting");
		System.out.println("");


		for(int i = 0; i < numOfWizzards; i++)
			p[i] = new Wizzards(i, apprenticies, mon);


		for(int i = 0; i < numOfWizzards; i++)
			try {
				p[i].t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		System.out.println("");
		System.out.println("the council is over");
	}
}
