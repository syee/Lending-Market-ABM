/**
 * 
 */
package model1;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

/**
 * @author stevenyee
 *
 */
public class InvestmentBank {
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	private double reserves;
	private double assets;
	private double liabilities;
	private double netWorth;
	private double loanRateFirms;
	private double interestToCB;
	private HashMap<Integer, LoanFromCB> loansFromCB;
	private HashMap<Integer, LoanToFirm> loansToFirms;
	
	public InvestmentBank(ContinuousSpace<Object> space, Grid<Object> grid, double reserves, double loanRateFirms, double interestToCB){
		this.space = space;
		this.grid = grid;
		this.reserves = reserves;
		this.loanRateFirms = loanRateFirms;
		this.interestToCB = interestToCB;
		
		addAssets(reserves);
		
		loansFromCB = new HashMap<Integer, LoanFromCB>();
		loansToFirms = new HashMap<Integer, LoanToFirm>();
	}
	
	public void addReserves(double amount){
		reserves += amount;
	}
	
	public void addAssets(double amount){
		assets += amount;
	}
	
	public void removeAssets(double amount){
		assets -= amount;
	}
	
	public void addLiabilities(double amount){
		liabilities += amount;
	}
	
	public void removeLiabilities(double amount){
		liabilities -= amount;
	}
	
	//how do I return reserves before killing bank?
		//add listener for bank reserves == -1?
	public double removeReserves(double amount){
		if (reserves <= -1){
			//destroy this bank
			
			return 0;
		}
		else{
			if (amount > (reserves + 1)){
				double lessThanFull = reserves;
				reserves = -1;
				return lessThanFull;
				//listener should destroy this bank			
			}
			else{
				reserves -= amount;
				return amount;
			}
		}
	}
	
	public boolean createLoanFirm(Firm debtor, double balance, double payment, int loanId){
		//I may want to incorporate reserve requirement type thing later
		if (balance <= reserves){
			removeReserves(balance);
			addAssets(balance);
			addLiabilities(balance);
			//this assumes payment has already been calculated correctly by firm
			LoanToFirm newLoan = new LoanToFirm(debtor, balance, payment, loanId);
			loansToFirms.put(loanId, newLoan);
			return true;
		}
		else{
			//investment bank decides to borrow money from commercial bank
			//
			///
			////
			/*
			/
			
			/
			/
			/
			/*/
			return false;
		}
	}
	
	//investment bank receiving payment from firm
	public boolean receivePayment(int loanId, double amount) throws Exception{
		if(loansToFirms.containsKey(loanId)){
			LoanToFirm thisLoan = loansToFirms.get(loanId);
			double paymentOutcome = thisLoan.receivePayment(amount);
			if (paymentOutcome == -1.0){
				addReserves(amount);
				removeAssets(amount);
				removeLiabilities(amount);
				//destroy this loan by removing it from map
				loansToFirms.remove(loanId);
				return true;
			}
			else if (thisLoan.getPayment() == paymentOutcome){
				//full payment made
				addReserves(amount);
				removeAssets(amount);
				removeLiabilities(amount);
				return true;
			}
			else{
				//less than full payment made
				//should I add a default counter?
				addReserves(amount);
				removeAssets(amount);
				removeLiabilities(amount);
				//now remove remaining loan balance from this bank's accounting
				double loss = thisLoan.getRemainingBalance();
				removeAssets(loss);
				removeLiabilities(loss);
				//destroy this loan
				loansToFirms.remove(loanId);
				return false;
			}			
		}
		else{
			throw new Exception("investment bank should not be receiving this payment");
		}
	}
	
	//making sure firms have paid up all loans
	public void checkAllLoans() throws Exception{
		Collection<LoanToFirm> loanList = loansToFirms.values();
		if (loanList != null){
			Iterator<LoanToFirm> loans = loanList.iterator();
			while (loans.hasNext()){
				LoanToFirm thisLoan = loans.next();
				if (!(thisLoan.isPaid())){
					int tempId = thisLoan.getId();
					receivePayment(tempId, 0.0);
					//this will destroy the loan since it is delinquent
				}
			}
		}		
	}
	
	

}
