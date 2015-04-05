package implementation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Queue;
import java.util.Scanner;
import java.util.StringTokenizer;

import iinterface.DatabaseManager;
import iinterface.WebNode;

public class DatabaseManagerImpl implements DatabaseManager {

	
	// Fields
	// ------
	
	private File databaseFile;
	private int breath;   // prefixed size of the table for temp URL links
	private static final int BREATH_DEFAULT = 100;  // Default Max Breath
	private static final String HEADER1 = "\tPriority\t\t\tURL";
	private static final String LINE = "-----------------------------------------------"
			+ "--------------------------------------------------------------";
	
	
	
	// Constructor
	// ----------
	
	public DatabaseManagerImpl(File file, int breath){
		this.breath = breath;
		this.databaseFile = file;
		wipeDatabase();   // wipe the file to be used as the database
		makeDatabaseHeader(); // add headers 
	}
	
	// getter/setters
	// --------------
	
	public void setDatabaseFile(File databaseFile){
		this.databaseFile = databaseFile;
	}
	
	public File getDatabaseFile(){
		return this.databaseFile;
	}
	
	
	public void setBreath(int breath){
		this.breath = breath;
	}
		
	
	// stringToWebNode(String s)
	// --------------------------
	
	@Override
	public WebNode stringToWebNode(String s) throws IllegalArgumentException {
		
		StringTokenizer st1 = new StringTokenizer(s,"\t");
		String tok1;
		String tok2;
		WebNode wn = null;
		
		if(st1.countTokens() == 2){
			tok1 = st1.nextToken();
			tok2 = st1.nextToken();
		}
		else{
			StringTokenizer st2 = new StringTokenizer(s," ");
			if(st2.countTokens() == 2){
				tok1 = st2.nextToken();
				tok2 = st2.nextToken();
			}
			else{
				throw new IllegalArgumentException();
			}		
		}
		
		try{
			int priorityNum = Integer.parseInt(tok1);
			String webPage = tok2;
			wn = new WebNodeImpl(webPage, priorityNum);
		}
		catch(IllegalArgumentException ex){
			ex.printStackTrace();
		}
	
		return wn;
	}


	
	// sizeOfTempTable()
	// -----------------
	@Override
	public int sizeOfTempTable() {
			
		BufferedReader br = null;
		int numWebNodes = -2;  // include two headers
		
		try{
			FileReader fr= new FileReader(databaseFile);
			br = new BufferedReader(fr);
			String line;
			
			while((line=br.readLine()) != null){
				if(line.equals("END")){
					//System.out.println("Find END");
					break;
				}
				else{
					//System.out.println(line);
					numWebNodes++;
					//System.out.println("numWebNodes " + numWebNodes);
				}
				
			}	
		}
		catch(FileNotFoundException ex1){
			ex1.printStackTrace();
		}
		catch(IOException ex2){
			ex2.printStackTrace();
		}
		finally{
			try{
				br.close();
				if(numWebNodes < 0)  // i.e just headers in the file and no webNodes.
					numWebNodes = 0; 
				return numWebNodes;
			}
			catch(IOException ex3){
				ex3.printStackTrace();
			}
		}
		
		return 0;
	}

	// printTempTable()
	// ----------------
	@Override
	public void printTempTable() {
		BufferedReader br = null;
	
		try{
			FileReader fr= new FileReader(databaseFile);
			br = new BufferedReader(fr);
			String line;
			
			while((line=br.readLine()) != null){
				if(line.equals("END")){
					//System.out.println("Find END");
					break;
				}
				else{
					System.out.println(line);
				}	
			}	
		}
		catch(FileNotFoundException ex1){
			ex1.printStackTrace();
		}
		catch(IOException ex2){
			ex2.printStackTrace();
		}
		finally{
			try{
				br.close();
			}
			catch(IOException ex3){
				ex3.printStackTrace();
			}
		}
	}
	
	
	// retrieveNextWebNode()
	// ---------------------
	@Override
	public WebNode retrieveNextWebNode() {
		boolean found = false;  //flag for finding first WebNode in temp Table of database
		File tempFile = new File("tempDatabase.txt");  // temp Database to write to with amendments
		FileWriter fw = null; 
		BufferedReader br = null;
		BufferedWriter bw = null;
		WebNode retrievedNode = null;  // retrieved WebNode
		
		
		try{
			FileReader fr= new FileReader(databaseFile);
			br = new BufferedReader(fr);
			fw = new FileWriter(tempFile); 
			bw = new BufferedWriter(fw);
			
			String line;
			
			line = br.readLine();  // copy and paste headers
			bw.write(line + "\n");
			line = br.readLine();
			bw.write(line + "\n");
			
			while((line=br.readLine()) != null){
				StringTokenizer st = new StringTokenizer(line);
				String tok1 = st.nextToken();
				if(!tok1.equals("0") && found == false){     // if found FIRST non-zero priority
					String tok2 = st.nextToken();
					String replaceLine = "\t0\t\t\t" + tok2;   // amend line
					bw.write(replaceLine + "\n");				    // write to file
					found = true;							// set flag
					retrievedNode = new WebNodeImpl(tok2,Integer.parseInt(tok1));  // create WebNode
				}
				else{
					bw.write(line + "\n");   // copy and paste line
				}						
			}		
		}
		catch(FileNotFoundException ex1){
			ex1.printStackTrace();
		}
		catch(IOException ex2){
			ex2.printStackTrace();
		}
		finally{
			try{
				br.close();
				bw.close();
				
				this.databaseFile = new File("tempDatabase.txt");
				return retrievedNode;
			}
			catch(IOException ex3){
				ex3.printStackTrace();
			}
		}
		return null;
	}

	@Override
	public boolean writeToTempTable(Queue<WebNode> q) {
		
		int sizeTable = sizeOfTempTable();
		int sizeQueue = q.size();
		boolean isTruncated = false;
		
		if(breath >= sizeTable)  // already full
			return false;
		else if(breath > sizeTable + sizeQueue){  // overflow
			int spareRoom = breath - sizeTable;
			Queue<WebNode> qTruncated = null;
			for(int i=0; i < spareRoom; i++){   // Truncate Queue to fit Table
				qTruncated.add(q.poll());
			}
			q = qTruncated;
			isTruncated = true;
		}
		else{							// Can Fit In Table
			BufferedReader br = null;;
			FileWriter fw;
			BufferedWriter bw = null;
			File tempFile = new File("tempDatabase.txt");  // temp Database to write to with amendments
			try{
				FileReader fr= new FileReader(databaseFile);
				br = new BufferedReader(fr);
				fw = new FileWriter(tempFile); 
				bw = new BufferedWriter(fw);
				
				String line;
				
				while((line=br.readLine()) != null){  
					//line = br.readLine();
					if(line.equals("END")){
						for(WebNode i : q){					// Add Queue to Bottom of Table
							String strNode = i.toString();
							bw.write(strNode + "\n");
						}
					} // end if
					else
						bw.write(line + "\n");   // copy and paste line
				} // end while
			} // end try
			catch(FileNotFoundException ex1){
				ex1.printStackTrace();
			}
			catch(IOException ex2){
				ex2.printStackTrace();
			}
			finally{
				try{
					br.close();
					bw.close();
					
					this.databaseFile = new File("tempDatabase.txt");
					if(isTruncated == false)
						return false;
					else 
						return true;
				}
				catch(IOException ex3){
					ex3.printStackTrace();
				}
			}
		}
		return false;  // dummy
	}  // end writeToTempTable()
	
	
	/**
	 * Create the Title Headers, "Priority" and "URL, for the database file 
	 */
	private void makeDatabaseHeader(){

		BufferedWriter bw = null;
				
		try{
			FileWriter fw = new FileWriter(this.databaseFile, true);  // true =  ammend
			bw = new BufferedWriter(fw);
			bw.write(HEADER1 + "\n");  // Headers for temp table
			bw.write(LINE + "\n");
			bw.write("END" + "\n");		// Marker for end of temp table
			bw.write(HEADER1 + "\n");  // Headers for Permanent table
			bw.write(LINE + "\n");
			
		}// end try
		catch (IOException ex1){
				System.out.println("File Not Writable: " + this.databaseFile.toString());
				ex1.printStackTrace();
		}
		finally{
			try{
				bw.close();
			}
			catch(IOException ex2){
				System.out.println("File Can't Be Closed");
			}
		}
	}
	
	
	/**
	 * Method to ensure that the database is wiped before each new WebCrawler class is instantiated.
	 */
	private void wipeDatabase(){
		try {
			new PrintWriter(this.databaseFile).close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	

} // end class
