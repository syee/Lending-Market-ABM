/**
 * 
 */
package model1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;

/**
 * @author stevenyee
 *
 */
public class CommercialBank {
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	private int age = 0;
	private double totalAssets;
	private double totalLiabilities;
	private double netWorth;

//	private double loanYears;
//	private double loanRate;
//	private double mortgagePaymentsIncoming;
//	private double loanTotal;
	
	//illiquidity stuff
	private double consumerShortTermPayout; //needs to be higher than bank value
	private double consumerLongTermPayout; // needs to be lower than bank value
	private double bankShortTermPayout; //needs to be lower than consumer value
	private double bankLongTermPayout; // needs to be higher than consumer value
	
	//differences are profits
	private double shortTermAssets;
	private double longTermAssets;
	
	private double shortTermLiabilities;
	private double longTermLiabilities;
	
	
	private HashSet<Consumer> Consumers;
//	private HashMap<String, LoanToIB> loansToIB;
	
	
	
	
	/** This method instantiates a Commercial Bank ("cBank").
	 * @param space
	 * @param grid
	 * @param reserves
	 * @param annualSavingsYield
	 * @param loanRate
	 * @param loanYears
	 * @throws Exception 
	 */
	public CommercialBank(ContinuousSpace<Object> space, Grid<Object> grid, double reserves, double consumerShortTermPayout, double consumerLongTermPayout, double bankShortTermPayout, double bankLongTermPayout) throws Exception{
		this.space = space;
		this.grid = grid;
		
		this.consumerShortTermPayout = consumerShortTermPayout;
		this.consumerLongTermPayout = consumerLongTermPayout;
		this.bankShortTermPayout = bankShortTermPayout;
		this.bankLongTermPayout = bankLongTermPayout;
		
		
//		this.loanRate = loanRate;
//		this.loanYears = loanYears;
//		this.mortgagePaymentsIncoming = 0.0;
//		this.loanTotal = 0.0;
		
		shortTermAssets  = reserves;
		Consumers = new HashSet<Consumer>();
//		loansToIB = new HashMap<String, LoanToIB>();
		
	}
	
	/** This method creates an account at this cBank for a consumer.
	 * If the consumer already has an account, nothing happens.
	 * @param holder Consumer that wishes to create an account
	 * @param amount Initial deposit is equal to Consumer's cash pile.
	 * @return return true if account created, returns false if consumer already has an account at this bank
	 * @throws Exception Throws exception if negative amount is given.
	 */
	public boolean addAccount(Consumer holder, double amount) throws Exception{
//		System.out.println("cBank trying to add " + holder + " with " + amount);
		if (!(Consumers.contains(holder))){
			Consumers.add(holder);
			shortTermAssets += amount;
			shortTermLiabilities += amount;
			getTotalAssets();
			getTotalLiabilities();
//			System.out.println("cBank successfully add " + holder + " with " + amount);
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
		if (Consumers.contains(holder)){
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
			if (Consumers.contains(holder)){
				shortTermAssets += amount;
				shortTermLiabilities += amount;
				getTotalAssets();
				getTotalLiabilities();
			}
			else{
				//consumer does not have account here
				//error
				throw new Exception("Consumer has no account here");
			}
		}
		else{
			throw new Exception("Consumer cannot deposit a negative amount!");
		}
	}

	
	public double getTotalAssets(){
		totalAssets = shortTermAssets + longTermAssets * bankShortTermPayout;
		return totalAssets;
	}
	
	public double getShortTermAssets(){
		return shortTermAssets;
	}
	
	public double getLongTermAssets(){
		return longTermAssets;
	}
	
	public double getTotalLiabilities(){
		totalLiabilities = shortTermLiabilities + longTermLiabilities * consumerShortTermPayout;
		return totalLiabilities;
	}
	
	public double getShortTermLiabilities(){
		return shortTermLiabilities;
	}
	
	public double getLongTermLiabilities(){
		return longTermLiabilities;
	}
	
//	public double getMortgagePaymentsIncoming(){
//		return mortgagePaymentsIncoming;
//	}
//	
//	public double getLoanTotal(){
//		return loanTotal;
//	}
	
//	/** This method adds a positive amount to cBank's assets.
//	 * @param amount Positive amount to be added.
//	 * @throws Exception Throws exception if amount is less than 0.
//	 */
//	public void addShortTermAssets(double amount) throws Exception{
//		if (amount >= 0.0){
//			shortTermAssets += amount;
//			shortTermLiabilities += amount;
//			getTotalAssets();
//		}
//		else{
//			throw new Exception("Cannot add negative amount to short term assets!");
//		}
//	}
	
//	/** This method removes a positive amount from cBank's assets
//	 * @param amount Positive amount to be removed.
//	 * @throws Exception Throws exception if amount is less than 0.
//	 */
//	public void removeAssets(double amount) throws Exception{
//		if (amount >= 0.0){
//			assets -= amount;
//		}
//		else{
//			throw new Exception("Cannot remove negative amount from assets!");
//		}
//	}
	
	public double removeShortTerm(double desiredAmount){
		double shortTermHeld = shortTermAssets;
		if (desiredAmount > shortTermHeld){
			shortTermAssets = 0;
			getTotalAssets();
			return shortTermHeld;
		}
		else{
			shortTermAssets -= desiredAmount;
			getTotalAssets();
			return desiredAmount;
		}
	}
	
	public double removeLongTerm(double desiredAmount){
		double longTermHeld = longTermAssets * bankShortTermPayout;
		if (desiredAmount > longTermHeld){
			longTermAssets = -1;
			getTotalAssets();
			return longTermHeld;
		}
		else{
			longTermAssets -= desiredAmount / bankShortTermPayout;
			getTotalAssets();
			return desiredAmount;
		}
	}
	
	public double consumerWithdraw(double desiredAmount) throws Exception{
		double leftOver = desiredAmount;
		System.out.println(this + " My leftover is " + leftOver);
		if (leftOver >= 0.0){
			leftOver -= removeShortTerm(leftOver);
			if (leftOver >= 0.0){
				System.out.println(this + " had to tap into its long term assets to cover the remaining " + leftOver);
				leftOver -= removeLongTerm(leftOver);
			}
			return desiredAmount - leftOver;
		}
		else{
			throw new Exception("Consumer cannot withdraw a negative amount!");
		}
	}
	
	public double consumerWithdrawShortTerm(Consumer holder, double desiredAmount) throws Exception{
		if (Consumers.contains(holder)){
			double amountReturned = consumerWithdraw(desiredAmount);
			shortTermLiabilities -= amountReturned;
			getTotalLiabilities();
			return amountReturned;
		}
		else{
			throw new Exception("Bank does not service consumer " + holder);
		}
	}
	
	public double consumerWithdrawLongTerm(Consumer holder, double desiredAmount) throws Exception{
		if (Consumers.contains(holder)){
			double amountReturned = consumerWithdraw(desiredAmount);
			longTermLiabilities -= amountReturned / consumerShortTermPayout;
			getTotalLiabilities();
			return amountReturned;
		}
		else{
			throw new Exception("Bank does not service consumer " + holder);
		}
	}
	
//	
//	
//	
//	/** This method adds a positive amount to cBank's liabilities.
//	 * @param amount Positive amount to be added.
//	 * @throws Exception Throws exception if amount is less than 0.
//	 */
//	public void addLiabilities(double amount) throws Exception{
//		if (amount >= 0.0){
//			liabilities += amount;
//		}
//		else{
//			throw new Exception("Cannot add negative amount to liabilities!");
//		}
//	}
	
//	/** This method removes a positive amount from cBank's liabilities
//	 * @param amount Positive amount to be added.
//	 * @throws Exception Throws exception if amount is less than 0.
//	 */
//	public void removeLiabilities(double amount) throws Exception{
//		if (amount >= 0.0){
//			liabilities -= amount;
//		}
//		else{
//			throw new Exception("Cannot add negative amount to reserves!");
//		}
//	}
	
	/** This method returns a cBank's net worth.
	 * @return Net worth of cBank.
	 */
	public double getNetWorth(){
		getTotalAssets();
		getTotalLiabilities();
		return totalAssets - totalLiabilities;
	}
	
	public int getAge(){
		return age;
	}
	
//	public double returnConsumerBalance(Consumer holder) throws Exception{
//		if (Consumers.containsKey(holder)){
//			return Consumers.get(holder);
//		}
//		else{
//			throw new Exception(holder + " does not have an account at this cBank");
//		}
//	}
	
	
//	//how do I return reserves before killing bank?
//	//add listener for bank reserves == -1?
//	/** This method removes a positive amount from a cBank's reserves.
//	 * If the cBank does not have enough reserves, it returns its full reserves. Its reserves are set to -1.0 to indicate the bank has gone bankrupt.
//	 * @param amount Positive amount the cBank must produce from its reserves.
//	 * @return Minimum of desired amount and cBank's reserves.
//	 * @throws Exception Throws exception if amount is less than 0.
//	 */
//	public double removeReserves(double amount) throws Exception{
//		if (amount >= 0.0){
//			if (reserves <= -1.0){
//				//destroy this bank
//				return 0.0;
//			}
//			else{
//				if (amount > (reserves)){
//					//cBank attempts to pull in all of its loans to pay off this deficit.
////					collectFullLoans();
//					if (amount > (reserves)){
//						double lessThanFull = reserves;
//						reserves = -1.0;
//						if (lessThanFull == -1.0){
//							lessThanFull = 0.0;
//							assets = 0.0;
//						}
//						else{
//							removeAssets(lessThanFull);
//						}
//						return lessThanFull;
//						//listener should destroy this bank. Maybe I run a destroy method if reserves = -1 at last "tick" method call
//					}
//					else{
//						reserves -= amount;
//						removeAssets(amount);
//						return amount;
//					}
//				}
//				else{
//					reserves -= amount;
//					removeAssets(amount);
//					return amount;
//				}
//			}
//		}
//		else{
//			throw new Exception("Cannot remove a negative amount from reserves!");
//		}
//	}
	
//	/** This method is how a consumer withdraws money from their cBank account. This method is only called by the Consumer object.
//	 * @param holder Consumer that owns account.
//	 * @param amount Positive amount Consumer wishes to withdraw.
//	 * @return Minimum of desired amount, amount in Consumer's account, and cBank reserves.
//	 * @throws Exception Throws Exception if amount is less than 0.
//	 */
//	public double withdraw(Consumer holder, double amount) throws Exception{
//		if (amount >= 0.0){
//			if (Consumers.containsKey(holder)){
////				System.out.print(this);
////				System.out.print(Consumers);
//				double savings = Consumers.get(holder);
//				if (savings >= amount){
//					//actualAmount must be equal to amount at this point. unnecessary checking?
//					double actualAmount = removeReserves(amount);
//					removeLiabilities(actualAmount);
//					savings -= actualAmount;
//					Consumers.put(holder, savings);
//					return actualAmount;
//				}
//				else{
//					Consumers.put(holder, 0.0);
//					double amountAvailable = removeReserves(savings);
//					removeLiabilities(amountAvailable);
//					//consumer should be removed because he could not pay a full debt
//					//this was changed so that the consumer will remove himself later in his scheduled method
////					Consumers.remove(holder);
//					//note that amountAvailable could be less than what consumer has if cBank is about to go bankrupt
//					return amountAvailable;
//				}
//			}
//			else{
//				throw new Exception ("Consumer does not have an account here!");
//			}
//		}
//		else{
//			throw new Exception ("Cannot withdraw a negative amount from an account!");
//		}
//	}
//	
//	/** This method pays interest on all Consumer accounts. It cycles through each account and then calls payInterest() for each account.
//	 * @throws Exception Throws an exception if any account has a negative balance.
//	 */
//	public void updateConsumers() throws Exception{
//		Set<Map.Entry<Consumer, Double>> consumerList = Consumers.entrySet();
//		if (consumerList != null){
//			Iterator<Map.Entry<Consumer, Double>> consumers = consumerList.iterator();
//			while (consumers.hasNext()){
//				Map.Entry<Consumer, Double> consumer = consumers.next();
//				payInterest(consumer.getKey());
//			}
//		}
//		
//	}
	
	
	
	
	
	
//	/** This method calculates and adds interest to a specific Consumer's account. Compounds monthly.
//	 * @param holder Consumer that owns account
//	 * @throws Exception Throws exception if holder balance is negative.
//	 */
//	public void payInterest(Consumer holder) throws Exception{
//		double savings = Consumers.get(holder);
//		//divide by 12 because each tick is a month
//		double interest = savings * (annualSavingsYield) / 12;
//		Consumers.put(holder, savings + interest);
//		addLiabilities(interest);
//	}
	
//	//commercial bank calculates how much it should be paid by an investment bank for potential loan
//	//I don't actually use this method in my first model. Maybe in later models
//	/** This method calculates what the monthly payment for a given balance should be.
//	 * This is based off of loanRate (the interest rate a bank charges), loanYears (the term of the mortgage), and the amount being sought.
//	 * This method actually does not get used by cBank in first model.
//	 * @param balance Amount to be borrowed.
//	 * @return Monthly payment.
//	 * @throws Exception Throws exception if balance is negative.
//	 */
//	public double calculateTickPayment(double balance) throws Exception{
//		if (balance >= 0.0){
//			double payment = balance/(1/loanRate)/(1-(1/Math.pow((1+loanRate),loanYears)))/12;
//			return payment;
//		}
//		else{
//			throw new Exception("Cannot calculate payment for a negative balance!");
//		}
//	}
	
//	//I may want to add methods in future where IB asks for a loan. Then CB comes back with payments. If IB can meet the payments, it takes the loan. Otherwise it keeps on looking
//	/** This method sees if a cBank can fulfill a loan request from an iBank.
//	 * The cBank makes the loan if has enough reserves to cover the requested balance. There is no reserve requirement.
//	 * @param debtor iBank requesting the loan.
//	 * @param balance Positive amount the iBank is requesting.
//	 * @param payment Monthly payment the iBank will make on the loan. This uses the same formula as cBank.calculateTickPayment().
//	 * @param loanId iD of the loan that the iBank is passed from a firm.
//	 * @return returns true if cBank creates the loan
//	 * @throws Exception Throws exception if balance is less than 0.
//	 */
//	public boolean createLoan(InvestmentBank debtor, double balance, double payment, String loanId) throws Exception{
//		//I may want to incorporate reserve requirement type thing later
//		if ((balance >= 0.0) && (payment >= 0.0)){
//			if (balance <= reserves){
//				//transforming balance so it will match payments * 12 * firmLoanYears
//				double totalPayment = calculateTickPayment(balance) * 12 * loanYears;
//				removeReserves(balance);
//				addShortTermAssets(totalPayment); //total amount plus interest iBank will pay
//				//this assumes payment has already been calculated correctly by investment bank
//				LoanToIB newLoan = new LoanToIB(debtor, totalPayment, payment, loanId);
//				loansToIB.put(loanId, newLoan);
//				mortgagePaymentsIncoming += payment;
//				loanTotal += totalPayment;
//				System.out.println("I am cBank " + this + ". I just loaned " + balance);
//				System.out.println("The monthly payment I will receive is" + payment);
//				return true;
//			}
//			else{
//				//commercial bank did not have enough in reserves to make this loan
//				return false;
//			}
//		}
//		else{
//			throw new Exception("Cannot create a negative loan!");
//		}
//	}
	
	
//	//commercial bank receiving payment from investment bank
//	/** This method is how cBanks receive loan payments from iBanks. This method is only called by LoanFromCB objects except in the event that a cBank identifies a delinquent loan.
//	 * If a cBank identifies a delinquent loan, it calls this method to receive a payment of 0.0 and then destroys the loan.
//	 * If the amount paid on the loan is not equal to the amount owed on the loan, the loan is delinquent and will be destroyed. 
//	 * @param tempId Id of the loan that the payment is being made on.
//	 * @param amount Positive amount that is being made on the loan.
//	 * @return returns true if the full payment owed was received.
//	 * @throws Exception Throws exception if any negative values.
//	 */
//	public boolean receivePayment(String tempId, double amount) throws Exception{
//		if (amount >= 0.0){
//			if(loansToIB.containsKey(tempId)){
//				System.out.println("I am the commercial bank accepting a loan payment with loan id " + tempId);
//				LoanToIB thisLoan = loansToIB.get(tempId);
//				double paymentOutcome = thisLoan.receivePayment(amount);
//				if (paymentOutcome == -4.0){
//					addReserves(amount);
//					removeAssets(amount);
//					mortgagePaymentsIncoming -= thisLoan.getPayment();
//					loanTotal -= amount;
//					loansToIB.remove(tempId);
//					return true;
//				}
//				else if (thisLoan.getPayment() == paymentOutcome){
//					//full payment made
//					addReserves(amount);
//					removeAssets(amount);
//					loanTotal -= amount;
//					return true;
//				}
//				else{
//					//less than full payment made
//					//should I add a default counter?
//					addReserves(paymentOutcome);
//					removeAssets(paymentOutcome);
//					loanTotal -= paymentOutcome;
//					mortgagePaymentsIncoming -= thisLoan.getPayment();
//					//now remove remaining loan balance from this bank's accounting
//					double loss = thisLoan.getRemainingBalance();
//					removeAssets(loss);
//					loanTotal -= loss;
//					//destroy this loan
//					loansToIB.remove(tempId);
//					return false;
//				}			
//			}
//			else{				
//				throw new Exception("Commercial bank should not be receiving this payment");
//			}
//		}
//		else{
//			throw new Exception("Cannot make a negative payment on a loan!");
//		}
//	}
	
//	//making sure investment banks have paid up all loans
//	//in theory, this method is made redundant by receivePayment()
//	//any loans that are not paid in full should be deleted in receivePayment()
//	/** This method searches for any delinquent loans that a cBank owns. This method is in theory redundant because all loans should have payments made on them.
//	 * If the payment made on a loan is less than the full amount, that loan should be removed in receivePayment().
//	 * @throws Exception Throws Exception if any loan balance is negative.
//	 */
//	public void checkAllLoans() throws Exception{
//		Collection<LoanToIB> loanList = loansToIB.values();
//		if (loanList != null){
//			Iterator<LoanToIB> loans = loanList.iterator();
//			while (loans.hasNext()){
//				LoanToIB thisLoan = loans.next();
//				if (!(thisLoan.isPaid())){
//					String tempId = thisLoan.getId();
//					receivePayment(tempId, 0.0);
//					//this will destroy the loan since it is delinquent
//					double loss = thisLoan.getRemainingBalance();
//					removeAssets(loss);
//					//destroy this loan
//					loans.remove();
//				}
//				else{
//					//loan payment has been made in full
//				}
//			}
//		}		
//	}
	
//	/** This method allows a cBank to call in all of its outstanding loan balances to prevent this cBank from going bankrupt.
//	 * This method is called whenever a cBank does not have enough reserves to meet a withdrawal request.
//	 * @throws Exception Throws exception if iBank attempts to make a negative payment on a loan.
//	 */
//	public void collectFullLoans() throws Exception{
//		Collection<LoanToIB> loanList = loansToIB.values();
//		if (loanList != null){
//			Iterator<LoanToIB> loans = loanList.iterator();
//			//this collects on all loans
//			while (loans.hasNext()){
//				LoanToIB thisLoan = loans.next();
//				String tempId = thisLoan.getId();
//				double balance = thisLoan.getRemainingBalance();
//				InvestmentBank iBank = thisLoan.getBank();
//				iBank.makeFullBalancePayment(tempId, balance);
//				//receivePayment(tempId, amountReceived); this happens in LoanFromCB.makePayment() called by iBank.makeLoanPayment
//				loansToIB.remove(tempId);
//			}
//		}
//		if (reserves == -1.0){
//			reserves = -2.0;
//		}
//	}
	
//	//This method is called when a cBank goes bankrupt to eliminate any lingering connections with iBanks.
//	public void deleteAllIBLoans() throws Exception{
//		Collection<LoanToIB> loanList = loansToIB.values();
//		if (loanList != null){
//			Iterator<LoanToIB> loans = loanList.iterator();
//			//this collects on all loans
//			while (loans.hasNext()){
//				LoanToIB thisLoan = loans.next();
//				String tempId = thisLoan.getId();
//				InvestmentBank iBank = thisLoan.getBank();
//				iBank.deleteLoan(tempId);
//				loansToIB.remove(tempId);
//			}
//		}
//	}
	
	public void transfers(){
		double tempBankShortTerm = shortTermAssets;
		shortTermAssets = longTermAssets * bankLongTermPayout;
		longTermAssets = tempBankShortTerm;
		getTotalAssets();
		
		double tempConsumerShortTerm = shortTermLiabilities;
		shortTermLiabilities = longTermLiabilities * consumerLongTermPayout;
		longTermLiabilities = tempConsumerShortTerm;
		getTotalLiabilities();		
	}
	
	
	public void cBankDie(){
		Context<Object> context = ContextUtils.getContext(this);
		context.remove(this);
	}
	
	/** This method removes all Consumers from a cBank.
	 * For each Consumer, it calls consumer.leaveBank() which removes references from the Consumer to the cBank.
	 * consumer.leaveBank() also calls removeAccount() which removes references from this cBank to the Consumer.
	 * @throws Exception 
	 * 
	 */
	public void removeAllConsumers() throws Exception{
		System.out.println("BANK BLOW UP " + this);
		Iterator<Consumer> consumerList = Consumers.iterator();
		if (consumerList != null){
			while (consumerList.hasNext()){
				Consumer consumer = consumerList.next();
				//I may want to pass an argument here later if more than one cBank per consumer
				consumer.forcedToLeaveBank(this);
				consumerList.remove();
			}
		}
	}
//	public void removeAllConsumers() throws Exception{
//		System.out.println("BANK BLOW UP " + this);
//		Set<Map.Entry<Consumer, Double>> consumerList = Consumers.entrySet();
//		if (consumerList != null){
//			Iterator<Map.Entry<Consumer, Double>> consumers = consumerList.iterator();
//			while (consumers.hasNext()){
//				Map.Entry<Consumer, Double> consumer = consumers.next();
//				//I may want to pass an argument here later if more than one cBank per consumer
//				consumer.getKey().forcedToLeaveBank(this);
//				consumers.remove();
//			}
//		}
//	}
	
	//this method should be unnecessary. More of an error checking method for removing delinquent loans
	/** This scheduled basic method checks all of a cBank's loans to make sure they have been paid in full.
	 * This method should be unnecessary as payments on all loans will be made before this method is called.
	 * @throws Exception
	 */
	@ScheduledMethod(start = 12, interval = 16)
	public void commBank_getPayments_12() throws Exception{
		//checkAllLoans();
	}
	
	/** This scheduled status method determines if a cBank has gone bankrupt or not.
	 * If it has gone bankrupt, it has already called collectFullLoans() so all loans have been destroyed.
	 * It now must remove all Consumer accounts.
	 * If the cBank is not bankrupt, it pays interest on all Consumer accounts.
	 * @throws Exception
	 */
	@ScheduledMethod(start = 15, interval = 16)
	public void commBank_check_15() throws Exception{
		if (longTermAssets <= -1){
			removeAllConsumers();
//			deleteAllIBLoans();
			//commercial bank goes bankrupt
			cBankDie();
		}
		else{
			//pay interest on all consumer accounts
			age++;
			transfers();
		}
//		System.out.println("I am cBank " + this);
//		System.out.println("I have this much in reserves " + getReserves());
//		System.out.println("I have this much in assets " + getAssets());
//		System.out.println("I have this much in liabilities " + getLiabilities());
//		System.out.println("I have this much in mortgagePayments " + getMortgagePaymentsIncoming());
//		System.out.println("I have this much in loan total" + getLoanTotal());
//		System.out.println("I have this much net worth" + getNetWorth());
	}


}
