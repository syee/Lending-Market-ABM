/**
 * 
 */
package model1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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
	private double loanYears;
	private double loanRate;
	private double mortgagePayments;
	
	private HashMap<Consumer, Double> Consumers;
	private HashMap<String, LoanToIB> loansToIB;
	
	
	
	
	/** This method instantiates a Commercial Bank ("cBank").
	 * @param space
	 * @param grid
	 * @param reserves
	 * @param annualSavingsYield
	 * @param loanRate
	 * @param loanYears
	 * @throws Exception 
	 */
	public CommercialBank(ContinuousSpace<Object> space, Grid<Object> grid, double reserves, double annualSavingsYield, double loanRate, double loanYears) throws Exception{
		this.space = space;
		this.grid = grid;
		this.reserves = reserves;
		this.annualSavingsYield = annualSavingsYield;
		this.loanRate = loanRate;
		this.loanYears = loanYears;
		this.mortgagePayments = 0.0;
		
		addAssets(reserves);
		Consumers = new HashMap<Consumer, Double>();
		loansToIB = new HashMap<String, LoanToIB>();
	}
	
	/** This method creates an account at this cBank for a consumer.
	 * If the consumer already has an account, nothing happens.
	 * @param holder Consumer that wishes to create an account
	 * @param amount Initial deposit is equal to Consumer's cash pile.
	 * @return return true if account created, returns false if consumer already has an account at this bank
	 * @throws Exception Throws exception if negative amount is given.
	 */
	public boolean addAccount(Consumer holder, double amount) throws Exception{
		if (!(Consumers.containsKey(holder))){
			Consumers.put(holder, amount);
			addReserves(amount);
			addLiabilities(amount);
			return true;
		}
		else{
			//Consumer already has account at bank
			return false;
		}
	}
	
	/** This method removes a Consumer's account from this cBank. This method is only called by consumer object.
	 * @param holder Consumer that is leaving the cBank.
	 * @return returns true if Consumer's account is removed, returns false if Consumer never had account here.
	 * @throws Exception Throws exception if amount is less than 0.
	 */
	public boolean removeAccount(Consumer holder) throws Exception{
		if (Consumers.containsKey(holder)){
			double savings = Consumers.get(holder); //this value should be zero
			removeReserves(savings);
			removeLiabilities(savings);
			Consumers.remove(holder);
			return true;
		}
		else{
			return false;
		}
	}
	
	/** This method allows a Consumer to add a positive amount to their account at this cBank. This method is only called from Consumer object.
	 * @param holder Consumer that owns account
	 * @param amount Positive amount consumer wishes to deposit
	 * @throws Exception Throws exception if amount is less than 0.
	 */
	public void deposit(Consumer holder, double amount) throws Exception{
		if (amount >= 0.0){
			if (Consumers.containsKey(holder)){
				double savings = Consumers.get(holder);
				savings += amount;
				addReserves(amount);
				addLiabilities(amount);
				Consumers.put(holder, savings);
			}
			else{
				//consumer does not have account here
				//error
				;
			}
		}
		else{
			throw new Exception("Consumer cannot deposit a negative amount!");
		}
	}
	
	/** This method adds a positive amount to a cBank's reserves.
	 * @param amount Positive amount to be added.
	 * @throws Exception Throws exception if amount is less than 0.
	 */
	public void addReserves(double amount) throws Exception{
		if (amount >= 0.0){
			reserves += amount;
		}
		else{
			throw new Exception("Cannot add negative amount to reserves!");
		}
	}
	
	/** This method adds a positive amount to cBank's assets.
	 * @param amount Positive amount to be added.
	 * @throws Exception Throws exception if amount is less than 0.
	 */
	public void addAssets(double amount) throws Exception{
		if (amount >= 0.0){
			assets += amount;
		}
		else{
			throw new Exception("Cannot add negative amount to assets!");
		}
	}
	
	/** This method removes a positive amount from cBank's assets
	 * @param amount Positive amount to be removed.
	 * @throws Exception Throws exception if amount is less than 0.
	 */
	public void removeAssets(double amount) throws Exception{
		if (amount >= 0.0){
			assets -= amount;
		}
		else{
			throw new Exception("Cannot remove negative amount from assets!");
		}
	}
	
	/** This method adds a positive amount to cBank's liabilities.
	 * @param amount Positive amount to be added.
	 * @throws Exception Throws exception if amount is less than 0.
	 */
	public void addLiabilities(double amount) throws Exception{
		if (amount >= 0.0){
			liabilities += amount;
		}
		else{
			throw new Exception("Cannot add negative amount to liabilities!");
		}
	}
	
	/** This method removes a positive amount from cBank's liabilities
	 * @param amount Positive amount to be added.
	 * @throws Exception Throws exception if amount is less than 0.
	 */
	public void removeLiabilities(double amount) throws Exception{
		if (amount >= 0.0){
			liabilities -= amount;
		}
		else{
			throw new Exception("Cannot add negative amount to reserves!");
		}
	}
	
	/** This method returns a cBank's net worth.
	 * @return Net worth of cBank.
	 */
	public double getNetWorth(){
		return assets - liabilities;
	}
	
	
	//how do I return reserves before killing bank?
	//add listener for bank reserves == -1?
	/** This method removes a positive amount from a cBank's reserves.
	 * If the cBank does not have enough reserves, it returns its full reserves. Its reserves are set to -1.0 to indicate the bank has gone bankrupt.
	 * @param amount Positive amount the cBank must produce from its reserves.
	 * @return Maximum of desired amount and cBank's reserves.
	 * @throws Exception Throws exception if amount is less than 0.
	 */
	public double removeReserves(double amount) throws Exception{
		if (amount >= 0.0){
			if (reserves <= -1.0){
				//destroy this bank
				return 0.0;
			}
			else{
				if (amount > (reserves)){
					//cBank attempts to pull in all of its loans to pay off this deficit.
					collectFullLoans();
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
				else{
					reserves -= amount;
					return amount;
				}
			}
		}
		else{
			throw new Exception("Cannot remove a negative amount from reserves!");
		}
	}
	
	/** This method is how a consumer withdraws money from their cBank account. This method is only called by the Consumer object.
	 * @param holder Consumer that owns account.
	 * @param amount Positive amount Consumer wishes to withdraw.
	 * @return Minimum of desired amount, amount in Consumer's account, and cBank reserves.
	 * @throws Exception Throws Exception if amount is less than 0.
	 */
	public double withdraw(Consumer holder, double amount) throws Exception{
		if (amount >= 0.0){
			double savings = Consumers.get(holder);
			if (savings >= amount){
				//actualAmount must be equal to amount at this point. unnecessary checking?
				double actualAmount = removeReserves(amount);
				removeLiabilities(actualAmount);
				savings -= actualAmount;
				Consumers.put(holder, savings);
				return amount;
			}
			else{
				Consumers.put(holder, 0.0);
				double amountAvailable = removeReserves(savings);
				removeLiabilities(amountAvailable);
				//consumer should be removed because he could not pay a full debt
				Consumers.remove(holder);
				//note that amountAvailable could be less than what consumer has if cBank is about to go bankrupt
				return amountAvailable;
			}
		}
		else{
			throw new Exception ("Cannot withdraw a negative amount from an account!");
		}
	}
	
	/** This method pays interest on all Consumer accounts. It cycles through each account and then calls payInterest() for each account.
	 * @throws Exception Throws an exception if any account has a negative balance.
	 */
	public void updateConsumers() throws Exception{
		Set<Map.Entry<Consumer, Double>> consumerList = Consumers.entrySet();
		if (consumerList != null){
			Iterator<Map.Entry<Consumer, Double>> consumers = consumerList.iterator();
			while (consumers.hasNext()){
				Map.Entry<Consumer, Double> consumer = consumers.next();
				payInterest(consumer.getKey());
			}
		}
		
	}
	
	/** This method calculates and adds interest to a specific Consumer's account. Compounds monthly.
	 * @param holder Consumer that owns account
	 * @throws Exception Throws exception if holder balance is negative.
	 */
	public void payInterest(Consumer holder) throws Exception{
		double savings = Consumers.get(holder);
		//divide by 12 because each tick is a month
		double interest = savings * (annualSavingsYield) / 12;
		Consumers.put(holder, savings + interest);
		addLiabilities(interest);
	}
	
	//commercial bank calculates how much it should be paid by an investment bank for potential loan
	//I don't actually use this method in my first model. Maybe in later models
	/** This method calculates what the monthly payment for a given balance should be.
	 * This is based off of loanRate (the interest rate a bank charges), loanYears (the term of the mortgage), and the amount being sought.
	 * This method actually does not get used by cBank in first model.
	 * @param balance Amount to be borrowed.
	 * @return Monthly payment.
	 * @throws Exception Throws exception if balance is negative.
	 */
	public double calculateTickPayment(double balance) throws Exception{
		if (balance <= 0.0){
			double payment = balance/(1/loanRate)/(1-(1/Math.pow((1+loanRate),loanYears)))/12;
			return payment;
		}
		else{
			throw new Exception("Cannot calculate payment for a negative balance!");
		}
	}
	
	//I may want to add methods in future where IB asks for a loan. Then CB comes back with payments. If IB can meet the payments, it takes the loan. Otherwise it keeps on looking
	/** This method sees if a cBank can fulfill a loan request from an iBank.
	 * The cBank makes the loan if has enough reserves to cover the requested balance. There is no reserve requirement.
	 * @param debtor iBank requesting the loan.
	 * @param balance Positive amount the iBank is requesting.
	 * @param payment Monthly payment the iBank will make on the loan. This uses the same formula as cBank.calculateTickPayment().
	 * @param loanId iD of the loan that the iBank is passed from a firm.
	 * @return returns true if cBank creates the loan
	 * @throws Exception Throws exception if balance is less than 0.
	 */
	public boolean createLoan(InvestmentBank debtor, double balance, double payment, String loanId) throws Exception{
		//I may want to incorporate reserve requirement type thing later
		if ((balance >= 0.0) && (payment >= 0.0)){
			if (balance <= reserves){
				removeReserves(balance);
				addAssets(payment*12*loanYears); //total amount plus interest iBank will pay
				//this assumes payment has already been calculated correctly by investment bank
				LoanToIB newLoan = new LoanToIB(debtor, balance, payment, loanId);
				loansToIB.put(loanId, newLoan);
				mortgagePayments += payment;
				return true;
			}
			else{
				//commercial bank did not have enough in reserves to make this loan
				return false;
			}
		}
		else{
			throw new Exception("Cannot create a negative loan!");
		}
	}
	
	
	//commercial bank receiving payment from investment bank
	/** This method is how cBanks receive loan payments from iBanks. This method is only called by LoanFromCB objects except in the event that a cBank identifies a delinquent loan.
	 * If a cBank identifies a delinquent loan, it calls this method to receive a payment of 0.0 and then destroys the loan.
	 * If the amount paid on the loan is not equal to the amount owed on the loan, the loan is delinquent and will be destroyed. 
	 * @param tempId Id of the loan that the payment is being made on.
	 * @param amount Positive amount that is being made on the loan.
	 * @return returns true if the full payment owed was received.
	 * @throws Exception Throws exception if any negative values.
	 */
	public boolean receivePayment(String tempId, double amount) throws Exception{
		if (amount >= 0.0){
			if(loansToIB.containsKey(tempId)){
				LoanToIB thisLoan = loansToIB.get(tempId);
				double paymentOutcome = thisLoan.receivePayment(amount);
				if (paymentOutcome == -1.0){
					addReserves(amount);
					removeAssets(amount);
					//destroy this loan by removing it from map
					loansToIB.remove(tempId);
					return true;
				}
				else if (thisLoan.getPayment() == paymentOutcome){
					//full payment made
					addReserves(amount);
					removeAssets(amount);
					return true;
				}
				else{
					//less than full payment made
					//should I add a default counter?
					addReserves(paymentOutcome);
					removeAssets(paymentOutcome);
					//now remove remaining loan balance from this bank's accounting
					double loss = thisLoan.getRemainingBalance();
					removeAssets(loss);
					//destroy this loan
					loansToIB.remove(tempId);
					return false;
				}			
			}
			else{
				throw new Exception("Commercial bank should not be receiving this payment");
			}
		}
		else{
			throw new Exception("Cannot make a negative payment on a loan!");
		}
	}
	
	//making sure investment banks have paid up all loans
	//in theory, this method is made redundant by receivePayment()
	//any loans that are not paid in full should be deleted in receivePayment()
	/** This method searches for any delinquent loans that a cBank owns. This method is in theory redundant because all loans should have payments made on them.
	 * If the payment made on a loan is less than the full amount, that loan should be removed in receivePayment().
	 * @throws Exception Throws Exception if any loan balance is negative.
	 */
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
					//destroy this loan
					loansToIB.remove(tempId);
				}
				else{
					//loan payment has been made in full
				}
			}
		}		
	}
	
	/** This method allows a cBank to call in all of its outstanding loan balances to prevent this cBank from going bankrupt.
	 * This method is called whenever a cBank does not have enough reserves to meet a withdrawal request.
	 * @throws Exception Throws exception if iBank attempts to make a negative payment on a loan.
	 */
	public void collectFullLoans() throws Exception{
		Collection<LoanToIB> loanList = loansToIB.values();
		if (loanList != null){
			Iterator<LoanToIB> loans = loanList.iterator();
			//this collects on all loans
			while (loans.hasNext()){
				LoanToIB thisLoan = loans.next();
				String tempId = thisLoan.getId();
				double balance = thisLoan.getRemainingBalance();
				InvestmentBank iBank = thisLoan.getBank();
				iBank.makeFullBalancePayment(tempId, balance);
				//receivePayment(tempId, amountReceived); this happens in LoanFromCB.makePayment() called by iBank.makeLoanPayment
				loansToIB.remove(tempId);
			}
		}
		if (reserves == -1.0){
			reserves = -2.0;
		}
	}
	
	/** This method removes all Consumers from a cBank.
	 * For each Consumer, it calls consumer.leaveBank() which removes references from the Consumer to the cBank.
	 * consumer.leaveBank() also calls removeAccount() which removes references from this cBank to the Consumer.
	 * @throws Exception 
	 * 
	 */
	public void removeAllConsumers() throws Exception{
		Set<Map.Entry<Consumer, Double>> consumerList = Consumers.entrySet();
		if (consumerList != null){
			Iterator<Map.Entry<Consumer, Double>> consumers = consumerList.iterator();
			while (consumers.hasNext()){
				Map.Entry<Consumer, Double> consumer = consumers.next();
				//I may want to pass an argument here later if more than one cBank per consumer
				consumer.getKey().leaveBank(this);
			}
		}
	}
	
	//this method should be unnecessary. More of an error checking method for removing delinquent loans
	/** This scheduled basic method checks all of a cBank's loans to make sure they have been paid in full.
	 * This method should be unnecessary as payments on all loans will be made before this method is called.
	 * @throws Exception
	 */
	public void commBank_getPayments_8() throws Exception{
		checkAllLoans();
	}
	
	/** This scheduled status method determines if a cBank has gone bankrupt or not.
	 * If it has gone bankrupt, it has already called collectFullLoans() so all loans have been destroyed.
	 * It now must remove all Consumer accounts.
	 * If the cBank is not bankrupt, it pays interest on all Consumer accounts.
	 * @throws Exception
	 */
	public void commBank_check_103() throws Exception{
		if (reserves <= -1){
			removeAllConsumers();
			//commercial bank goes bankrupt
		}
		else{
			//pay interest on all consumer accounts
			updateConsumers();
		}
	}


}
