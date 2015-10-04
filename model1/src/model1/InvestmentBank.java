/**
 * 
 */
package model1;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.UUID;

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
	private CommercialBank cBank = null;
	private double cBLoanYears;
	private double firmLoanYears;
	
	private HashMap<String, LoanFromCB> loansFromCB;
	private HashMap<String, LoanToFirm> loansToFirms;	
	private HashMap<String, LoanToFirm> waitingLoans;
	
	public InvestmentBank(ContinuousSpace<Object> space, Grid<Object> grid, double reserves, double loanRateFirms, double interestToCB, double cBLoanYears, double firmLoanYears){
		this.space = space;
		this.grid = grid;
		this.reserves = reserves;
		this.loanRateFirms = loanRateFirms;
		this.interestToCB = interestToCB;
		this.cBLoanYears = cBLoanYears;
		this.firmLoanYears = firmLoanYears;
		
		addAssets(reserves);
		
		loansFromCB = new HashMap<String, LoanFromCB>();
		loansToFirms = new HashMap<String, LoanToFirm>();
		waitingLoans = new HashMap<String, LoanToFirm>();
	}
	
	public void joinBank(CommercialBank cBank){
		if(cBank.addIBAccount(this)){
			this.cBank = cBank;
			//change this to allow more than one commercial bank in future
		}
		else{
			//already joined bank
		}
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
			//destroy this investment bank
			return 0.0;
		}
		else{
			if (amount > (reserves + 1)){
				double lessThanFull = reserves;
				reserves = -1.0;
				return lessThanFull;
				//listener should destroy this investment bank			
			}
			else{
				reserves -= amount;
				return amount;
			}
		}
	}
	
	//investment bank tries to borrow money from its single commercial bank
	public boolean requestLoanCB(double balance, double payment, String loanId){
		if (cBank != null){
			if(cBank.createLoan(this, balance, payment, loanId)){
				addReserves(balance);
				addLiabilities(payment);
				addAssets(payment);
				LoanFromCB newLoan = new LoanFromCB(cBank, balance, payment, loanId);
				loansFromCB.put(loanId,  newLoan);
			}
			else{
				//commercial bank did not have enough reserves to make loan
				;
			}
		}
		//no commercial bank relationship exists
		return false;
	}
		
	
	//investment banks cycles through its outstanding loan payments and pays them, calls makeLoanPayment()
	public void makeLoanPayments() throws Exception{
		Collection<LoanFromCB> loanList = loansFromCB.values();
		if (loanList != null){
			Iterator<LoanFromCB> loans = loanList.iterator();
			while (loans.hasNext()){
				LoanFromCB thisLoan = loans.next();
				String tempId = thisLoan.getId();
				double paymentDue = thisLoan.getPayment();
				double actualPayment = removeReserves(paymentDue);
				
				if (makeLoanPayment(tempId, actualPayment)){
					//full payment made
					;
				}
				else{
					//investment bank should be destroyed since it failed to make full payment
					//appears to be handled in makeLoanPayment
					;
				}
			}
		}
	}
	
	//investment bank calculates how much the tick payment on a loan it should collect from a firm
	public double calculateFirmTickPayment(double amount){
		double payment = amount/(1/loanRateFirms)/(1-(1/Math.pow((1+loanRateFirms),firmLoanYears)))/12;
		return payment;
	}
	
	//investment bank calculates how much it should pay commercial bank for potential loan
	public double calculateCBTickPayment(double amount){
		double payment = amount/(1/interestToCB)/(1-(1/Math.pow((1+interestToCB),cBLoanYears)))/12;
		return payment;
	}
	
	
	//investment banks pays back commercial bank for one loan
	public boolean makeLoanPayment(String tempId, double amount) throws Exception{
		if(loansFromCB.containsKey(tempId)){
			LoanFromCB thisLoan = loansFromCB.get(tempId);
			double paymentOutcome = thisLoan.makePayment(amount);
			if (paymentOutcome == -1.0){
				//	removeReserves(amount); this already happens
				removeAssets(amount);
				removeLiabilities(amount);
				//destroy this loan by removing it from map
				loansFromCB.remove(tempId);
				return true;
			}
			else if (thisLoan.getPayment() == paymentOutcome){
				//full payment made
				removeReserves(amount);
				removeAssets(amount);
				removeLiabilities(amount);
				return true;
			}
			else{
				//less than full payment made
				//should I add a default counter?
				removeReserves(amount);
				removeAssets(amount);
				removeLiabilities(amount);
				//now remove remaining loan balance from this bank's accounting
				double loss = thisLoan.getRemainingBalance();
				removeAssets(loss);
				removeLiabilities(loss);
				//destroy this investment bank
				///
				///
				// THIS BANK NEEDS TO BE DESTROYED
				//
				//
				////
				loansFromCB.remove(tempId);
				return false;
			}
		}
		else{
			throw new Exception("Investment Bank should not be making this payment to this Commercial Bank ");
		}
	}
	
	
	//investment bank cycles through its wait list loans and calls requestLoanCB()
	public void borrowWaitingLoans() throws Exception{
		Collection<LoanToFirm> loanList = waitingLoans.values();
		if (loanList != null){
			Iterator<LoanToFirm> loans = loanList.iterator();
			while (loans.hasNext()){
				LoanToFirm thisLoan = loans.next();
				//creating loan ID for investment bank from commercial bank
				String newLoanId =  UUID.randomUUID().toString();
				requestLoanCB(thisLoan.getRemainingBalance(), thisLoan.getPayment(), newLoanId);
			}
		}		
	}
	
	//investment bank sees if it has borrowed enough money to lend to firms
	public void resolveWaitingLoans() throws Exception{
		Collection<LoanToFirm> loanList = waitingLoans.values();
		if (loanList != null){
			Iterator<LoanToFirm> loans = loanList.iterator();
			while (loans.hasNext()){
				LoanToFirm thisLoan = loans.next();
				createLoanFirm(thisLoan.getFirm(), thisLoan.getRemainingBalance(), thisLoan.getPayment(), thisLoan.getId());
			}
			//removing all loans from waiting loan list now
			Collection<LoanToFirm> toDeleteLoanList = waitingLoans.values();
			Iterator<LoanToFirm> loansDelete = toDeleteLoanList.iterator();
			while (loansDelete.hasNext()){
				LoanToFirm thisLoan = loansDelete.next();
				waitingLoans.remove(thisLoan.getId());
			}
		}
	}
	
	//investment bank checks to see if a loan passed by checking loanId. interfaces with firm
	public boolean checkLoanStatus(String loanId){
		if (loansToFirms.get(loanId) != null){
			return true;
		}
		else{
			return false;
		}
	}
	
	
	public boolean createLoanFirm(Firm debtor, double balance, double payment, String loanId){
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
			//investment bank tries to borrow money from commercial bank
			
			//adds this loan to list of loans to try to reconcile
			LoanToFirm newLoan = new LoanToFirm(debtor, balance, payment, loanId);
			waitingLoans.put(loanId, newLoan);
			
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
	public boolean receivePayment(String tempId, double amount) throws Exception{
		if(loansToFirms.containsKey(tempId)){
			LoanToFirm thisLoan = loansToFirms.get(tempId);
			double paymentOutcome = thisLoan.receivePayment(amount);
			if (paymentOutcome == -1.0){
				addReserves(amount);
				removeAssets(amount);
				removeLiabilities(amount);
				//destroy this loan by removing it from map
				loansToFirms.remove(tempId);
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
				loansToFirms.remove(tempId);
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
					String tempId = thisLoan.getId();
					receivePayment(tempId, 0.0);
					//this will destroy the loan since it is delinquent
				}
			}
		}		
	}
	
	

}
