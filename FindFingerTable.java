import java.util.Vector;            
public class FindFingerTable extends Algorithm {

    public Object run() {
        return findFingersAndKeys(getID());
    }
	
	/* Reads the configuration file to determine the keys to store locally, the keys to search for and to construct 
	   the finger table for this processor                                                                         */
	   
	/* ----------------------------- */
    public Object findFingersAndKeys(String id) {
	/* ------------------------------ */
       try {
			Vector<Integer> searchKeys; 		// Keys that this processor needs to find in the P2P system
			Vector<Integer> localKeys;   		// Keys to store in this processor
			Vector<Integer> fingers;            // Addresses to store in the finger table
			String[] fingerTable;               // finger table for this processor
			int m;                              // The ring of identifiers has size 2^m

			localKeys = new Vector<Integer>();
			fingers = new Vector<Integer>();
			searchKeys = keysToFind();          // Read information from configuration file 
			
			if (searchKeys.size() > 0) {
				for (int i = 0; i < searchKeys.size();) {
					if (searchKeys.elementAt(i) < 0) {   	// Negative keys are the keys that must be stored locally
						localKeys.add(-searchKeys.elementAt(i));
						searchKeys.remove(i);
					}
					else if (searchKeys.elementAt(i) > 1000) {
						fingers.add(searchKeys.elementAt(i)-1000);
						searchKeys.remove(i);
					}
					else ++i;  // Key needs to be searched for
				}
			}
			
			m = fingers.size();
			// Store the processor addresses in the finger table 
			fingerTable = new String[m+1];
			for (int i = 0; i < m; ++i) fingerTable[i] = integerToString(fingers.elementAt(i));
			fingerTable[m] = id;
			
			String s = "Processor "+id+". Keys stored locally: ";
			for (int i = 0; i < localKeys.size(); ++i) s = s + localKeys.elementAt(i)+" ";
			printMessage (s);
			
			s = "Processor "+id+". Keys to search for: ";
			for (int i = 0; i < searchKeys.size(); ++i) s = s + searchKeys.elementAt(i)+" ";
			printMessage (s);
			
			s = "Processor "+id+". Finger table: ";
			for (int i = 0; i <= m; ++i) s = s + "finger["+i+"]="+fingerTable[i]+" ";
			printMessage(s);

			// Synchronous loop
			while (waitForNextRound()) {
				return "";
			}
                
 
        } catch(SimulatorException e){
            System.out.println("ERROR: " + e.toString());
        }
    
        return null;
    }
}