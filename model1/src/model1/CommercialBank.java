/**
 * 
 */
package model1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

/**
 * @author stevenyee
 *
 */
public class CommercialBank {
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	private double reserves;
	private double assets;
	private double liabilities;
	private double netWorth;
	private double annualSavingsYield;
	private double periodSavingsYield;
	private double loanYears;
	private double loanRate;
	
	private ArrayList<InvestmentBank> iBanks;
	private HashMap<Consumer, Double> Consumers;
	private HashMap<String, LoanToIB> loansToIB;
	
	
	
	
	public CommercialBank(ContinuousSpace<Object> space, Grid<Object> grid, double reserves, double savingsYield, double loanRate, double loanYears){
		this.space = space;
		this.grid = grid;
		this.reserves = reserves;
		this.periodSavingsYield = savingsYield;
		this.loanRate = loanRate;
		this.loanYears = loanYears;
		
		addAssets(reserves);
		periodSavingsYield = savingsYield/12;
		Consumers = new HashMap<Consumer, Double>();
		loansToIB = new HashMap<String, LoanToIB>();
		iBanks = new ArrayList<InvestmentBank>();
	}
	
	public boolean addIBAccount(InvestmentBank iBank){
		if(!(iBanks.contains(iBank))){
			iBanks.add(iBank);
			return true;
		}
		else{
			//investment bank already has relationship
			return false;
		}
	}
	
	public boolean addAccount(Consumer holder, double amount){
		if (!(Consumers.containsKey(holder))){
			Consumers.put(holder, amount);
			addReserves(amount);
			addAssets(amount);
			addLiabilities(amount);
			return true;
		}
		else{
			//Consumer already has account at bank
			return false;
		}
	}
	
	public void deposit(Consumer holder, double amount){
		if (Consumers.containsKey(holder)){
			double savings = Consumers.get(holder);
			savings += amount;
			addReserves(amount);
			addAssets(amount);
			addLiabilities(amount);
			Consumers.put(holder, savings);
		}
		else{
			//consumer does not have account here
			//error
			;
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
	
	public double getNetWorth(){
		return reserves - liabilities;
	}
	
	
	//how do I return reserves before killing bank?
	//add listener for bank reserves == -1?
	public double removeReserves(double amount){
		if (reserves <= -1){
			//destroy this bank
			
			return 0.0;
		}
		else{
			if (amount > (reserves)){
				double lessThanFull = reserves;
				reserves = -1.0;
				return lessThanFull;
				//listener should destroy this bank. Maybe I run a destroy method if reserves = -1 at last "tick" method call		
			}
			else{
				reserves -= amount;
				return amount;
			}
		}
	}
	
	public double withdraw(Consumer holder, double amount){
		double savings = Consumers.get(holder);
		if (savings >= amount){
			//actualAmount must be equal to amount at this point. unnecessary checking?
			double actualAmount = removeReserves(amount);
			removeAssets(actualAmount);
			removeLiabilities(actualAmount);
			savings -= actualAmount;
			Consumers.put(holder, savings);
			return amount;
		}
		else{
			Consumers.put(holder, 0.0);
			double amountAvailable = removeReserves(savings);
			removeAssets(amountAvailable);
			removeLiabilities(amountAvailable);
			//consumer should be removed because he could not pay a full debt
			Consumers.remove(holder);
			//note that amountAvailable could be less than what consumer has if cBank is about to go bankrupt
			return amountAvailable;
		}
	}
	
	public void payInterest(Consumer holder){
		double savings = Consumers.get(holder);
		//divide by 12 because each tick is a month
		double interest = savings * (periodSavingsYield) / 12;
		Consumers.put(holder, savings + interest);
		addLiabilities(interest);
		//should I also add this to the assets?
		addAssets(interest);
	}
	
	//commercial bank calculates how much it should be paid by an investment bank for potential loan
	//I don't actually use this method in my first model. Maybe in later models
	public double calculateTickPayment(double amount){
		double payment = amount/(1/loanRate)/(1-(1/Math.pow((1+loanRate),loanYears)))/12;
		return payment;
	}
	
	//I may want to add methods in future where IB asks for a loan. Then CB comes back with payments. If IB can meet the payments, it takes the loan. Otherwise it keeps on looking
	public boolean createLoan(InvestmentBank debtor, double balance, double payment, String loanId){
		//I may want to incorporate reserve requirement type thing later
		if (balance <= reserves){
			removeReserves(balance);
			addAssets(balance);
			addLiabilities(balance);
			//this assumes payment has already been calculated correctly by investment bank
			LoanToIB newLoan = new LoanToIB(debtor, balance, payment, loanId);
			loansToIB.put(loanId, newLoan);
			return true;
		}
		else{
			//commercial bank did not have enough in reserves to make this loan
			return false;
		}
	}
	
	
	//commercial bank receiving payment from investment bank
	public boolean receivePayment(String tempId, double amount) throws Exception{
		if(loansToIB.containsKey(tempId)){
			LoanToIB thisLoan = loansToIB.get(tempId);
			double paymentOutcome = thisLoan.receivePayment(amount);
			if (paymentOutcome == -1.0){
				addReserves(amount);
				removeAssets(amount);
				removeLiabilities(amount);
				//destroy this loan by removing it from map
				loansToIB.remove(tempId);
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
				loansToIB.remove(tempId);
				return false;
			}			
		}
		else{
			throw new Exception("Commercial bank should not be receiving this payment");
		}
	}
	
	//making sure investment banks have paid up all loans
	//in theory, this method is made redundant by receivePayment()
	//any loans that are not paid in full should be deleted in receivePayment()
	public void checkAllLoans() throws Exception{
		Collection<LoanToIB> loanList = loansToIB.values();
		if (loanList != null){
			Iterator<LoanToIB> loans = loanList.iterator();
			while (loans.hasNext()){
				LoanToIB thisLoan = loans.next();
				if (!(thisLoan.isPaid())){
					String tempId = thisLoan.getId();
					receivePayment(tempId, 0.0);
					//this will destroy the loan since it is delinquent
					double loss = thisLoan.getRemainingBalance();
					removeAssets(loss);
					removeLiabilities(loss);
					//destroy this loan
					loansToIB.remove(tempId);
				}
				else{
					//loan payment has been made in full
				}
			}
		}		
	}


}
