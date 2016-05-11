package hw09;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// TO DO: Task is currently an ordinary class.
// You will need to modify it to make it a task,
// so it can be given to an Executor thread pool.
//
class Task implements Runnable {
    private static final int A = constants.A;
    private static final int Z = constants.Z;
    private static final int numLetters = constants.numLetters;

    private Account[] accounts;
    private String transaction;

    ArrayList<Integer> cachedAccounts;
    ArrayList<Boolean> readAccounts;
    ArrayList<Boolean> writeAccounts;
    ArrayList<Boolean> openAccounts;
    public Task(Account[] allAccounts, String trans) {
        accounts = allAccounts;
        transaction = trans;
        initializeAccounts();
    }
    
    // TO DO: parseAccount currently returns a reference to an account.
    // You probably want to change it to return a reference to an
    // account *cache* instead.
    //
    
    /**
     * initializes/resets the lists of modes of accounts
     */
    private void initializeAccounts() {
    	readAccounts = new ArrayList<>(Collections.nCopies(numLetters, false));
    	writeAccounts = new ArrayList<>(Collections.nCopies(numLetters, false));
    	cachedAccounts = new ArrayList<>(Collections.nCopies(numLetters, 0));
    	openAccounts = new ArrayList<>(Collections.nCopies(numLetters, false));
    }
    
    private Account parseAccount(String name) {
        int accountNum = (int) (name.charAt(0)) - (int) 'A';
        
        if (accountNum < A || accountNum > Z)
            throw new InvalidTransactionError();
        
        Account a = accounts[accountNum];
        
        for (int i = 1; i < name.length(); i++) {
            if (name.charAt(i) != '*')
                throw new InvalidTransactionError();
            accountNum = (accounts[accountNum].peek() % numLetters);
            a = accounts[accountNum];
        }
        
        return a;
    }

    private int parseAccountOrNum(String name) {
        int rtn;
        if (name.charAt(0) >= '0' && name.charAt(0) <= '9') {
            rtn = new Integer(name).intValue();
        } else {
            rtn = parseAccount(name).peek();
            readAccounts.add(rtn, true);
            try {
            	cachedAccounts.add(rtn, accounts[rtn].peek());
            	readAccounts.add(rtn, true);
            	rtn = cachedAccounts.get(rtn);
            } catch (TransactionUsageError t){
            	t.printStackTrace();
            }
        }
        return rtn;
    }
    
    /**
     * open the accounts in order to read and write transactions
     * @throws TransactionAbortException
     */
    private void openAccounts() throws TransactionAbortException {
   
    	for (int i = 0; i < numLetters; i++) {
    		if (readAccounts.get(i)) {
    			if (writeAccounts.get(i)) {
    				accounts[i].open(true);
    				openAccounts.add(i, true);
    			}
    			else {
    				accounts[i].open(false);
    				openAccounts.add(i, true);
    				accounts[i].verify(cachedAccounts.get(i));
    			}
    		}
    	}
    }
    
    /**
     * close the accounts if any accounts are open from read/write
     * @param mode the boolean value for closing write accounts
     * @throws TransactionAbortException
     */
    private void closeAccounts(boolean mode) throws TransactionAbortException {
    	for (int i = 0; i < numLetters; i++) {
    		if (openAccounts.get(i)) {
    			accounts[i].close();
    			openAccounts.add(i, false);
    			if (mode)
    				writeAccounts.add(i, false);
    		}
    	}
    }

    public void run() {
        // tokenize transaction
        String[] commands = transaction.split(";");

        for (int i = 0; i < commands.length; i++) {
            String[] words = commands[i].trim().split("\\s");
            if (words.length < 3)
                throw new InvalidTransactionError();
            
            Account lhs = parseAccount(words[0]);
            int rhs = 0;
            
            writeAccounts.add(lhs.getValue(), true);
            while (true) {
            	int index = lhs.getValue();
            	try {
            		cachedAccounts.add(index, accounts[index].peek());
            		readAccounts.add(index, true);
            	} catch (TransactionUsageError t) {
            		t.printStackTrace();
            	}
            
            if (!words[1].equals("="))
                throw new InvalidTransactionError();
            
            int rhs = parseAccountOrNum(words[2]);
            for (int j = 3; j < words.length; j+=2) {
                if (words[j].equals("+"))
                    rhs += parseAccountOrNum(words[j+1]);
                else if (words[j].equals("-"))
                    rhs -= parseAccountOrNum(words[j+1]);
                else
                    throw new InvalidTransactionError();
            }
            try {
                lhs.open(true);
            } catch (TransactionAbortException e) {
                // won't happen in sequential version
            }
            lhs.update(rhs);
            lhs.close();
        }
        }
        System.out.println("commit: " + transaction);
    }
}

public class Server {

	// requires: accounts != null && accounts[i] != null (i.e., accounts are properly initialized)
	// modifies: accounts
	// effects: accounts change according to transactions in inputFile
    public static void runServer(String inputFile, Account accounts[])
        throws IOException {

        // read transactions from input file
        String line;
        BufferedReader input =
            new BufferedReader(new FileReader(inputFile));

        // TO DO: you will need to create an Executor and then modify the
        // following loop to feed tasks to the executor instead of running them
        // directly.  
        
        ExecutorService exec = Executors.newFixedThreadPool(5);
        
        while ((line = input.readLine()) != null) {
            Task t = new Task(accounts, line);
            //t.run();
            exec.execute(t);
        }
        
        input.close();

    }
}
