public class EvolutionaryProcessBatch implements Runnable{
    int begin; int batchSize; EvolutionaryProcess[] masterArr;

    public EvolutionaryProcessBatch(int begin, int batchSize, EvolutionaryProcess[] masterArr){
	this.begin = begin;
	this.batchSize = batchSize;
	this.masterArr = masterArr;
    }
    public void run(){
	int endInd = Math.min(begin+batchSize, masterArr.length);
	for (int i = begin; i < endInd; i++){
	    //	    System.out.println(i);

	    masterArr[i].run();
	}
    }
}