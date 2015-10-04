/**
 * 
 */
package model1;

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
	private HashMap<Consumer, Double> Consumers;
	private double annualSavingsYield;
	private double periodSavingsYield;
	private double loanRate;
	private HashMap<Integer, LoanToIB> loansToIB;
	
	
	
	
	public CommercialBank(ContinuousSpace<Object> space, Grid<Object> grid, double reserves, double savingsYield, double loanRate){
		this.space = space;
		this.grid = grid;
		this.reserves = reserves;
		this.periodSavingsYield = savingsYield;
		this.loanRate = loanRate;
		addAssets(reserves);
		periodSavingsYield = savingsYield/12;
		Consumers = new HashMap<Consumer, Double>();
		loansToIB = new HashMap<Integer, LoanToIB>();
	}
	
	public boolean addAccount(Consumer holder, double amount){
		if(amount == (Consumers.put(holder, amount))){
			addReserves(amount);
			addAssets(amount);
			addLiabilities(amount);
			return true;
		}
		else{
			return false;
		}
	}
	
	public void deposit(Consumer holder, double amount){
		double savings = Consumers.get(holder);
		savings += amount;
		addReserves(amount);
		addAssets(amount);
		addLiabilities(amount);
		Consumers.put(holder, savings);		
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
	
	public double withdraw(Consumer holder, double amount){
		double savings = Consumers.get(holder);
		if (savings >= amount){
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
			return amountAvailable;
		}
	}
	
	public void payInterest(Consumer holder){
		double savings = Consumers.get(holder);
		double interest = savings * (periodSavingsYield);
		Consumers.put(holder, savings + interest);
		addLiabilities(interest);
		//should I also add this to the assets?
		addAssets(interest);
	}
	
	//I may want to add methods in future where IB asks for a loan. Then CB comes back with payments. If IB can meet the payments, it takes the loan. Otherwise it keeps on looking
	public boolean createLoan(InvestmentBank debtor, double balance, double payment, int loanId){
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
	public boolean receivePayment(int loanId, double amount) throws Exception{
		if(loansToIB.containsKey(loanId)){
			LoanToIB thisLoan = loansToIB.get(loanId);
			double paymentOutcome = thisLoan.receivePayment(amount);
			if (paymentOutcome == -1.0){
				addReserves(amount);
				removeAssets(amount);
				removeLiabilities(amount);
				//destroy this loan by removing it from map
				loansToIB.remove(loanId);
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
				loansToIB.remove(loanId);
				return false;
			}			
		}
		else{
			throw new Exception("Commercial bank should not be receiving this payment");
		}
	}
	
	//making sure investment banks have paid up all loans
	public void checkAllLoans() throws Exception{
		Collection<LoanToIB> loanList = loansToIB.values();
		if (loanList != null){
			Iterator<LoanToIB> loans = loanList.iterator();
			while (loans.hasNext()){
				LoanToIB thisLoan = loans.next();
				if (!(thisLoan.isPaid())){
					int tempId = thisLoan.getId();
					receivePayment(tempId, 0.0);
					//this will destroy the loan since it is delinquent
				}
			}
		}		
	}


}
