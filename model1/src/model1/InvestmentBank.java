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
	
	public boolean joinBank(CommercialBank cBank){
		if(cBank.addIBAccount(this)){
			this.cBank = cBank;
			//change this to allow more than one commercial bank in future
			return true;
		}
		else{
			//already joined the bank
			return false;
		}
	}
	
	public boolean leaveBank(CommercialBank cBankDone) throws Exception{
		if (cBank == cBankDone){
			//pay off any loans to the bank
			Collection<LoanFromCB> loanList = loansFromCB.values();
			if (loanList != null){
				Iterator<LoanFromCB> loans = loanList.iterator();
				while (loans.hasNext()){
					LoanFromCB thisLoan = loans.next();
					String tempId = thisLoan.getId();
					if (thisLoan.getBank() == cBankDone){
						double balance = thisLoan.getRemainingBalance();
						makeLoanPayment(tempId, balance);
						loansFromCB.remove(thisLoan);
					}
				}
			}
			//this works for a single cBank per iBank
			cBank = null;
			return true;
		}
		else{
			return false;	
		}
	}
	
	//I may want to adjust this later to return a list of banks
	public CommercialBank getBank(){
		return cBank;
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
			if (amount > reserves){
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
	//I may want to expand this for multiple commercial banks in the future
	//I may want to incorporate waiting list similar to firms in the future
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
	public void payBackAllLoans() throws Exception{
		Collection<LoanFromCB> loanList = loansFromCB.values();
		if (loanList != null){
			Iterator<LoanFromCB> loans = loanList.iterator();
			while (loans.hasNext()){
				LoanFromCB thisLoan = loans.next();
				String tempId = thisLoan.getId();
				double paymentDue = thisLoan.getPayment();
				double actualPayment = removeReserves(paymentDue);
				
				if (makeLoanPayment(tempId, actualPayment) == paymentDue){
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
	public double makeLoanPayment(String tempId, double amount) throws Exception{
		if(loansFromCB.containsKey(tempId)){
			LoanFromCB thisLoan = loansFromCB.get(tempId);
			double paymentOutcome = thisLoan.makePayment(amount);
			if (paymentOutcome == -1.0){
				//	removeReserves(amount); this already happens
				removeAssets(amount);
				removeLiabilities(amount);
				//destroy this loan by removing it from map
				loansFromCB.remove(tempId);
				return amount;
			}
			else if (thisLoan.getPayment() == paymentOutcome){
				//full payment made
				removeAssets(amount);
				removeLiabilities(amount);
				return amount;
			}
			else{
				//less than full payment made
				//should I add a default counter?
				removeAssets(paymentOutcome);
				removeLiabilities(paymentOutcome);
				//now remove remaining loan balance from this bank's accounting
				double loss = thisLoan.getRemainingBalance();
				removeAssets(loss);
				removeLiabilities(loss);
				//destroy this investment bank
				// THIS BANK NEEDS TO BE DESTROYED
				////
				loansFromCB.remove(tempId);
				return paymentOutcome;
			}
		}
		else{
			throw new Exception("Investment Bank should not be making this payment to this Commercial Bank ");
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
			/*/
			return false;
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
				//String newLoanId =  UUID.randomUUID().toString();
				//I am just going to use the loan ID that was already created
				if (cBank != null){
					requestLoanCB(thisLoan.getRemainingBalance(), thisLoan.getPayment(), thisLoan.getId());
				}
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
			removeAllWaitingLoans();
		}
	}
	
	public void removeAllWaitingLoans(){
		Collection<LoanToFirm> toDeleteLoanList = waitingLoans.values();
		Iterator<LoanToFirm> loansDelete = toDeleteLoanList.iterator();
		while (loansDelete.hasNext()){
			LoanToFirm thisLoan = loansDelete.next();
			waitingLoans.remove(thisLoan.getId());
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
					double loss = thisLoan.getRemainingBalance();
					removeAssets(loss);
					removeLiabilities(loss);
					//destroy this loan
					loansToFirms.remove(tempId);
				}
			}
		}		
	}
	
	public void removeAllLoansFromCB() throws Exception{
		Collection<LoanFromCB> loanList = loansFromCB.values();
		if (loanList != null){
			Iterator<LoanFromCB> loans = loanList.iterator();
			while (loans.hasNext()){
				LoanFromCB thisLoan = loans.next();
				removeSingleLoanFromCB(thisLoan);
			}
		}
	}
	
	public void removeSingleLoanFromCB(LoanFromCB thisLoan) throws Exception{
		//this condition is necessary if this method is called because a firm is going bankrupt
		//this may be unnecessary if I force each firm to default on its loans individually
		//when a firm defaults on its loans, I can make a payment of 0 which would remove this loan from this iBank already
		if(loansFromCB.containsValue(thisLoan)){
			String tempId = thisLoan.getId();
			makeLoanPayment(tempId, 0);
			double balance = thisLoan.getRemainingBalance();
			removeAssets(balance);
			removeLiabilities(balance);
			loansFromCB.remove(tempId);
		}
		else{
			;
		}
	}
	
	public void removeAllLoansToFirms() throws Exception{
		Collection<LoanToFirm> loanList = loansToFirms.values();
		if (loanList != null){
			Iterator<LoanToFirm> loans = loanList.iterator();
			while (loans.hasNext()){
				LoanToFirm thisLoan = loans.next();
				removeSingleLoanToFirm(thisLoan);
			}
		}
	}
	
	public void removeSingleLoanToFirm(LoanToFirm thisLoan) throws Exception{
		if(loansToFirms.containsValue(thisLoan)){
			String tempId = thisLoan.getId();
			receivePayment(tempId, 0.0);
			double balance = thisLoan.getRemainingBalance();
			removeAssets(balance);
			removeLiabilities(balance);
			loansToFirms.remove(tempId);
		}
		else{
			;
		}
	}
	
	public void collectFullLoans() throws Exception{
		Collection<LoanToFirm> loanList = loansToFirms.values();
		if (loanList != null){
			Iterator<LoanToFirm> loans = loanList.iterator();
			//this collects on all loans
			while (loans.hasNext()){
				LoanToFirm thisLoan = loans.next();
				String tempId = thisLoan.getId();
				double balance = thisLoan.getRemainingBalance();
				Firm firm = thisLoan.getFirm();
				double amountReceived = firm.makeLoanPayment(tempId, balance);
				receivePayment(tempId, amountReceived);
				loansToFirms.remove(tempId);
			}
		}
		if (reserves == -1){
			reserves = -2;
		}
	}
	
	public void invBank_getPayments_6() throws Exception{
		checkAllLoans();
	}

	public void invBank_makePayments_7() throws Exception{
		payBackAllLoans();
	}
	
	public void invBank_receiveRequests_2() throws Exception{
		if (cBank == null){
			//search for a bank
			//joinBank()
		}
		borrowWaitingLoans();
	}
	
	//iBank lends out money to firms that were waiting
	public void invBank_borrowFunds_3() throws Exception{
		resolveWaitingLoans();
	}
	
	public void invBank_check_102() throws Exception{
		if (reserves <= -1){
			removeAllLoansFromCB();
			//this method should be redundant as collectFullLoans() should already have been called
			removeAllLoansToFirms();
			//this method should also be unnecessary given the timing of this scheduled method
			removeAllWaitingLoans();
		}
	}
	
	//I want to turn this into a listener
	public void invBank_trouble_listener() throws Exception{
		if (reserves <= -1){
			collectFullLoans();
		}
	}
	


}
