package com;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.TreeMap;

public class BracketedTaxEngine {
		
	
	private List<TaxNode> taxNodes = new ArrayList<>();
	private TreeMap<Long,Integer> indexIntoTaxNodes = new TreeMap<>();
	
	
/*MAIN METHOD*/
	public static void main (String[] args) {
	    BracketedTaxEngine  engine = new BracketedTaxEngine ();
		engine.readTaxBracketsFileIntoDataStructure ();
		List<Double> incomes = new ArrayList<>();
		/*Read the List of Incomes for which Tax got to be calculated from the Console*/
		Scanner scanner = new Scanner (System.in);
		while(scanner.hasNextLine()) {
			String line =scanner.nextLine();
			if (line.equalsIgnoreCase("done")) {
				scanner.close(); break;
			}
			//Ignoring Exception checks for now. 
		 incomes.add(Double.valueOf(line));
 	}
		
		
	Map<Double,Double> incomeToTax=engine.calculateTotalTaxForIncomes(incomes);
	for (Map.Entry<Double, Double> entry : incomeToTax.entrySet()) {
		System.out.println("Income:"+ entry.getKey()+ "   ====>Tax Amount:"+entry.getValue());
	}
		
	}
	
	private   Map<Double,Double> calculateTotalTaxForIncomes(List<Double> incomes) {
		 Map<Double,Double> result = new HashMap<>();
		for (Double income:incomes) {
			if (!result.containsKey(income))
		result.put(income, calculateTax(income)); 
		}
		return result;
	}
	
	
	/*For now,  these Rules are applied  for the Calculation   (NOTE :But as mentioned above, Caching of Data and its dynamic application for calculation can be enabled)
- The portion of the income that is less than $10k is untaxed
- The portion of the income that is less than $20k is taxed at 10%
- The portion of the income that is less than $50k is taxed at 20%
- Any portion of the income that is above $50k is taxed at 30%

	 * */
	
	private   Double calculateTax (Double income) {	
		Long incomeL = income.longValue();
	int taxNodeListIndex=	prepareAllTaxBracketsForConsideration(incomeL);
		//Now that we have the sorted Data Structure, based off "beginning" amount, read all the data upto that index and use those brackets for calculation.
	Double finalTaxableAmount = 0.0;
	Long beginning =0l;
	Long end = 0l;
	TaxNode node  =null;
	for (int i=0;i<=taxNodeListIndex;i++) {
		 node =taxNodes.get(i);
		beginning= node.getBeginning();
		end = node.getEnd();
		if (end<beginning) {
			end =income.longValue();
		}
		Long taxable = end-beginning;
		if (i==taxNodeListIndex) {
			taxable = incomeL - beginning;
		}

		finalTaxableAmount += (taxable *(node.getPercentage())/100);
	}
	
	return finalTaxableAmount;
	}
	

	
	class TaxNode{	
		Long beginning;
		Long end;
		Double percentage;
		
		public TaxNode (Long beginning,Long end,Double percentage) {
			this.beginning=beginning;
			this.end=end;
			this.percentage=percentage;
		}
		public Long getBeginning() {
			return beginning;
		}
		public void setBeginning(Long beginning) {
			this.beginning = beginning;
		}
		public Long getEnd() {
			return end;
		}
		public void setEnd(Long end) {
			this.end = end;
		}
		public Double getPercentage() {
			return percentage;
		}
		public void setPercentage(Double percentage) {
			this.percentage = percentage;
		}
				
	}
	
	class TaxNodeComparator implements Comparator<TaxNode>{
		@Override
		public int compare(TaxNode o1, TaxNode o2) {
			return  o1.getBeginning().compareTo( o2.getBeginning());	
		}		
	}


	/*Populate  a Data Structure (LinkedList used here) sorted as per the Beginning Bracket Value of the Node  (assuming that the data file is not in sorted orcer)
	 * Here is  how I envision the data file :
	 * 10000 20000 10
     * 20000 50000 20
     * 50000 0     30

	 * Here for each new TaxNode object , its fields will have : beginning = 10000 end= 199999 Percentage =10.00
	 * Add these to the List : taxNodes
	 * Also create a Map with pointers to reach the right index in the List based off "beginning" value:  indexIntoTaxNodes (TreeMap to enable sorting)
	 * 
	 */
		
	/*=========== SPECIAL NOTE ==========
	 *  * THINGS to be considered  (These are not taken care of in the code written here , as of today.)
     *    1)Caching the Tax Brackets data  (beginning, end, percentage)
     *    2)Ability to dynamically change the value in the Cache , update/create/delete the data structure  at the right places 
     *    3)Exception Case scenarios 
     *    4)Multi-Threading Scenarios and code safety before it can be enabled. 
     *    A Caching solution with easy APIs to do these operations are preferred (data structure might change in this case)
     *    or
     *    we can manually, make the changes to the Data Structure (a separate program that could be run to read the cache to make the apt changes)
     *    Care should be taken to  make sure the code is not reading the cache at that point of time, doing the calculation based off stale data in the cache. 
     *    In other words, Cache Eviction timers need to be set properly in all the clients.
     *    Once the caching is in place, All instances of the code (run multiple instances for scalability) can read the cache, get the brackets Information and perform the calculation accordingly.
	 * */
	 private  void readTaxBracketsFileIntoDataStructure(){
			
		 //For now, reading the file from the same directory as this java file.
		 InputStream in = BracketedTaxEngine.class.getResourceAsStream("TaxBrackets.txt");
		 BufferedReader br = new BufferedReader(new InputStreamReader(in));
		 String st =null;
		 StringTokenizer tokenizer = null;
		 int index=0;
		 try {
			while (( st = br.readLine()) != null){
				 tokenizer = new StringTokenizer(st);
				Long beginning =Long.valueOf(tokenizer.nextToken());
				Long end =Long.valueOf(tokenizer.nextToken())-1;
				Double  percentage = Double.valueOf(tokenizer.nextToken());
				taxNodes.add(new TaxNode(beginning,end,percentage));
				  }
		} catch (IOException e) {
			// TODO 
		}
		 
		 /*Sort the List of Tax Nodes (populated and added to the List in the exact order as it was read from the un-sorted file)
		  based on "beginning value" in ASC  */
		 Collections.sort(taxNodes, new  TaxNodeComparator());
		 
		 //Store the Index of the Tax Nodes into the TreeMap (automatic sorting based off Key Value in ASC order) to get to all the Tax Brackets to be considered for Tax Calculation.
		 for (TaxNode node: taxNodes) {
			 indexIntoTaxNodes.put(node.getBeginning(), index++);
		 }
		    }
	
	private  int prepareAllTaxBracketsForConsideration (Long income) {
		int taxNodesListIndex = 0;
		Long taxBracketToBeConsidered = 0l;
		if (indexIntoTaxNodes.containsKey(income)) {
			taxNodesListIndex = indexIntoTaxNodes.get(income);
		}
		else {
			taxBracketToBeConsidered =indexIntoTaxNodes.floorKey(income);
			taxNodesListIndex = indexIntoTaxNodes.get(taxBracketToBeConsidered);
		}		
		
		return taxNodesListIndex;
	}

}
