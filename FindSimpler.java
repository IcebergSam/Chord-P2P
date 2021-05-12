import java.util.Vector;            
public class FindSimpler extends Algorithm {
    private final int m = 6;                   	// Ring of identifiers has size 2^m
    private int SizeRing = exp(2,m);
	String result = ""; 						// Locations of searched-for keys will be stored here 

    public Object run() {
        return find(getID());
    }

	// Each message sent by this algorithm has the form: flag, key, ID 
	// where:
	// - if flag = "GET" then the message is a request to get the document with the given key and send it to processor ID
	// - if flag = "LOOKUP" then the message must be forwarded to another processor to find the key 
	// - if flag = "FOUND" then the message contains the key and processor that stores it
	// - if flag = "NOT_FOUND" then the requested key is not in the system
	// - if flag = "END" the algorithm terminates
	
	/* Method implements a simple algorithm to find a key in a peer-to peer system assuming that each
	   processor only knows the address of its successor. The algorithm will return a String
	   of the form: "k1:p1 k2:p2 ... kr:pr" where k1, k2, ... kr are the keys that this processor
	   needs to find and pi is the processor that stores the document with key ki for each 
	   i = 1, 2, ... r                                                                            */
	/* ----------------------------- */
    public Object find(String id) {
	/* ------------------------------ */
       try {
			String succ = successor();      	// Successor of this processor in the ring of identifiers
			Message mssg = null, message;		// Message buffers
			int indexNextKey = 0;
			boolean keyProcessed = false;   	// This variable takes value true when the algorithm has
												// determined that the current key being searched either
												// is in the system or it is not in the system
			int keyValue;                   	// Key that is being searched
			String[] data;
			
			Vector<Integer> tmpKeys;
			Vector<Integer> searchKeys; 		// Keys that this processor needs to find in the P2P system
			Vector<Integer> localKeys;   		// Keys stored in this processor			
			localKeys = new Vector<Integer>();
			searchKeys = new Vector<Integer>();
			tmpKeys = keysToFind();             // Read from the configuration file the keys to store locally, the keys to 
			                                    // find and and processors id's for the finger table
			
			getKeys(tmpKeys,searchKeys,localKeys);  // Identify the keys that must be stored locally and the keys that
			                                        // the algorithm has to find 
			int hashID = hp(id);            	// Ring identifier for this processor
			int hashSucc = hp(succ);			// Ring identifier for the successor of this processor
			
			if (searchKeys.size() > 0) { 		// Get the next key to find and check if it is stored locally
				keyValue = searchKeys.elementAt(0);
				searchKeys.remove(0);           // Do not search for the same key twice
				if (localKeys.contains(keyValue)) {
					result = result + keyValue + ":" + id + " "; // Store location of key in the result
					keyProcessed = true;
				}
				else // Key was not stored locally
					if (inSegment(hk(keyValue), hashID, hashSucc)) // Check if key must be stored in successor
						mssg = makeMessage (succ, pack("GET",keyValue,id));
					else mssg = makeMessage (succ, pack("LOOKUP",keyValue,id)); // Key is not in successor
			}

			// Synchronous loop
			while (waitForNextRound()) {
				if (mssg != null) {
					send(mssg);
					data = unpack(mssg.data());
					if (data[0].equals("END") && searchKeys.size() == 0) return result;
				}
				mssg = null;
				message = receive();
				while (message != null) {
					data = unpack(message.data());
					if (data[0].equals("GET")) {
						// If this is the same GET message that this processor originally sent, then the
						// key is not in the system
						if (data[2].equals(id)) {
							result = result + data[1] + ":not found ";
							keyProcessed = true;
						}
						// This processor must contain the key, if it is in the system
						else if (localKeys.contains(stringToInteger(data[1])))
							mssg = makeMessage(data[2],pack("FOUND",data[1],id));
						else mssg = makeMessage(data[2],pack("NOT_FOUND",data[1]));
					}
					else if (data[0].equals("LOOKUP")) {
						// Forward the request
						keyValue = stringToInteger(data[1]);
						if (inSegment(hk(keyValue),hashID,hashSucc))
							mssg = makeMessage (succ,pack("GET",keyValue,data[2]));
						else mssg = makeMessage (succ,pack("LOOKUP",keyValue,data[2]));
					}
					else if (data[0].equals("FOUND")) {
						result = result + data[1] + ":" + data[2] + " ";
						keyProcessed = true;
					}
					else if (data[0].equals("NOT_FOUND")) {
						result = result + data[1] + ":not found ";
						keyProcessed = true;
					}
					else if (data[0].equals("END")) 
							if (searchKeys.size() > 0) return result;
							else mssg = makeMessage(succ,"END");
					message = receive();
				}
				
				if (keyProcessed) { // Search for the next key
					if (searchKeys.size() == 0)  // There are no more keys to find 
						mssg = makeMessage(succ,"END");
					else {
						keyValue = searchKeys.elementAt(0);
						searchKeys.remove(0);  // Do not search for same key twice
						if (localKeys.contains(keyValue)) {
							result = result + keyValue + ":" + id + " "; // Store location of key in the result
							keyProcessed = true;
						}				
						else if (inSegment(hk(keyValue), hashID, hashSucc)) // Check if key must be in successor
								mssg = makeMessage (succ, pack("GET",keyValue,id));
							else mssg = makeMessage (succ, pack("LOOKUP",keyValue,id)); // Key is not in successor
					}
					if (mssg != null) keyProcessed = false;
				}
	    }
                
 
        } catch(SimulatorException e){
            System.out.println("ERROR: " + e.toString());
        }
    
        /* At this point something likely went wrong. If you do not have a result you can return null */
        return null;
    }


	/* Determine the keys that need to be stored locally and the keys that the processor needs to find.
	   Negative keys returned by the simulator's method keysToFind() are to be stored locally in this 
       processor as positive numbers.                                                                    */
	/* ---------------------------------------------------------------------------------------------------- */
	private void getKeys (Vector<Integer> tmpKeys, Vector<Integer> searchKeys, Vector<Integer> localKeys) throws SimulatorException {
	/* ---------------------------------------------------------------------------------------------------- */
		String local = "";  // Keys to be stored locally in this processor
		if (numKeysToFind() > 0) {
			while (tmpKeys.size() > 0) {
				if (tmpKeys.elementAt(0) < 0) {   	// Negative keys are the keys that must be stored locally
					localKeys.add(-tmpKeys.elementAt(0));
					tmpKeys.remove(0);
				}
				else if (tmpKeys.elementAt(0) > 1000)
					tmpKeys.remove(0);  // These are processor id's to store in the finger table. These entries
					                    // are deleted because this algorithm does not use the finger table
				else {
					searchKeys.add(tmpKeys.elementAt(0)); // These are the keys that the algorithm must find
					tmpKeys.remove(0);
				}
			}
		}
		for (int i = 0; i < localKeys.size(); ++i) local = local + localKeys.elementAt(i) + " ";
		showMessage(local); // Show in the simulator the keys stored in this processor
	}
	
    /* Determine whether hk(value) is in (hp(ID),hp(succ)] */
    /* ---------------------------------------------------------------- */
    private boolean inSegment(int hashValue, int hashID, int hashSucc) {
    /* ----------------------------------------------------------------- */
		if (hashID == hashSucc)
			if (hashValue == hashID) return true;
			else return false;
        else if (hashID < hashSucc) 
			if ((hashValue > hashID) && (hashValue <= hashSucc)) return true;
			else return false;
		else 
			if (((hashValue > hashID) && (hashValue < SizeRing)) || 
                ((0 <= hashValue) && (hashValue <= hashSucc)))  return true;
			else return false;
    }

    /* Hash function for processors. We use the simple mod function */
    /* ------------------------------- */
    private int hp(String ID) throws SimulatorException{
	/* ------------------------------- */
        return stringToInteger(ID) % SizeRing;
    }

    /* Hash function for processors. We use the simple mod function */
    /* ------------------------------- */
    private int hk(int key) {
    /* ------------------------------- */
        return key % SizeRing;
    }

    /* Compute base^exponent ("base" to the power "exponent") */
    /* --------------------------------------- */
    private int exp(int base, int exponent) {
    /* --------------------------------------- */
        int i = 0;
        int result = 1;

		while (i < exponent) {
			result = result * base;
			++i;
		}
		return result;
    }
}
