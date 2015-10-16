/**
 * 
 */
package model1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.SpatialMath;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;
import repast.simphony.util.SimUtilities;

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
	private Random rand;
	
	private HashMap<String, LoanFromCB> loansFromCB;
	private HashMap<String, LoanToFirm> loansToFirms;	
	private HashMap<String, LoanToFirm> waitingLoans;
	
	/** This method instantiates an InvestmentBank object.
	 * @param space
	 * @param grid
	 * @param reserves
	 * @param loanRateFirms
	 * @param interestToCB
	 * @param cBLoanYears
	 * @param firmLoanYears
	 * @throws Exception 
	 */
	public InvestmentBank(ContinuousSpace<Object> space, Grid<Object> grid, double reserves, double loanRateFirms, double interestToCB, double cBLoanYears, double firmLoanYears) throws Exception{
		this.space = space;
		this.grid = grid;
		this.reserves = reserves;
		this.loanRateFirms = loanRateFirms;
		this.interestToCB = interestToCB;
		this.cBLoanYears = cBLoanYears;
		this.firmLoanYears = firmLoanYears;
		this.rand = new Random();
		
		addAssets(reserves);
		
		loansFromCB = new HashMap<String, LoanFromCB>();
		loansToFirms = new HashMap<String, LoanToFirm>();
		waitingLoans = new HashMap<String, LoanToFirm>();
	}
	
	/** This method allows an iBank to create a relationship with a cBank.
	 * @param cBankNew The cBank the iBank wishes to join.
	 * @return Returns true if no previous relationship between this cBank and iBank existed.
	 */
	public boolean joinBank(CommercialBank cBankNew){
		if (cBankNew == null){
			return false;
		}
		if(cBank != cBankNew){
			cBank = cBankNew;
			//change this to allow more than one commercial bank in future
			return true;
		}
		else{
			//already joined the bank
			return false;
		}
	}
	
	/** This method allows an iBank to leave a cBank.
	 * The iBank must first pay off all of its loans to that cBank to sever the relationship.
	 * @param cBankDone cBank the iBank wishes to leave.
	 * @return Returns true if successful.
	 * @throws Exception
	 */
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
						makeFullBalancePayment(tempId, balance);
//						loansFromCB.remove(thisLoan);
						loans.remove();
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
	/** This method returns an iBank's singular cBank.
	 * @return cBank.
	 */
	public CommercialBank getBank(){
		return cBank;
	}
	
	/** This method adds a positive value to an iBank's reserves.
	 * @param amount Positive amount to be added.
	 * @throws Exception 
	 */
	public void addReserves(double amount) throws Exception{
		if (amount >= 0.0){
			reserves += amount;
		}
		else{
			throw new Exception("Cannot add a negative amount to iBank reserves!");
		}
	}
	
	/** This method adds a positive value to an iBank's assets.
	 * @param amount Positive amount to be added.
	 * @throws Exception 
	 */
	public void addAssets(double amount) throws Exception{
		if (amount >= 0.0){
			assets += amount;
		}
		else{
			throw new Exception("Cannot add negative amount to iBank assets!");
		}
	}
	
	/** This method removes a positive amount from iBank's assets.
	 * @param amount Positive amount to be removed.
	 * @throws Exception
	 */
	public void removeAssets(double amount) throws Exception{
		if (amount >= 0.0){
			assets -= amount;
		}
		else{
			throw new Exception("Cannot remove negative amount from iBank assets!");
		}
	}
	
	/** This method adds a positive amount to an iBank's liabilities.
	 * @param amount Positive amount to be added.
	 * @throws Exception
	 */
	public void addLiabilities(double amount) throws Exception{
		if (amount >= 0.0){
			liabilities += amount;
		}
		else{
			throw new Exception("Cannot add negative amount to iBank liabilities!");
		}
	}
	
	/** This method removes a positive amount from an iBank's liabilities.
	 * @param amount Positive amount to be removed.
	 * @throws Exception
	 */
	public void removeLiabilities(double amount) throws Exception{
		if (amount >= 0.0){
			liabilities -= amount;
		}
		else{
			throw new Exception("Cannot remove negative amount from iBank liabilities!");
		}
	}
	
	//how do I return reserves before killing bank?
	//add listener for bank reserves == -1?
	/** This method removes a positive amount from an iBank's reserves.
	 * If the iBank does not have enough in reserves to cover a withdrawal, it calls in all outstanding loan balances.
	 * If the iBank still does not have enough in reserves, it goes bankrupt.
	 * @param amount Positive amount to be removed.
	 * @return Minimum of amount and reserves.
	 * @throws Exception 
	 */
	public double removeReserves(double amount) throws Exception{
		if (amount >= 0.0){
			if (reserves <= -1){
				//destroy this investment bank
				return 0.0;
			}
			else{
				if (amount > reserves){
					collectFullLoans();
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
				else{
					reserves -= amount;
					return amount;
				}
			}
		}
		else{
			throw new Exception("Cannot remove a negative value from iBank reserves!");
		}
	}
	
	//investment bank tries to borrow money from its single commercial bank
	//I may want to expand this for multiple commercial banks in the future
	//I may want to incorporate waiting list similar to firms in the future
	/** This method requests a loan from a cBank.
	 * All arguments are passed from Firm originating loan request.
	 * @param balance Positive amount to be borrowed from cBank.
	 * @param payment Positive amount iBank will pay monthly to cBank.
	 * @param loanId ID of loan that will match a loan from an iBank to a firm.
	 * @return Returns true if successful.
	 * @throws Exception
	 */
	public boolean requestLoanCB(double balance, String loanId) throws Exception{
		if (balance >= 0.0){
			if (cBank != null){
				double paymentCB = calculateCBTickPayment(balance);
				if(cBank.createLoan(this, balance, paymentCB, loanId)){
					addReserves(balance);
					addLiabilities(balance);
					double paymentFirm = calculateFirmTickPayment(balance);
					LoanFromCB newLoan = new LoanFromCB(cBank, balance, paymentFirm, loanId);
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
		else{
			throw new Exception("Cannot borrow negative amount from cBank!");
		}
	}
	
	//this method removes loans that are fully paid off
	public void checkLoansForPaid(){
		Collection<LoanFromCB> loanList = loansFromCB.values();
		if (loanList != null){
			Iterator<LoanFromCB> loans = loanList.iterator();
			while (loans.hasNext()){
				LoanFromCB thisLoan = loans.next();
				if (thisLoan.getRemainingBalance() <= 0.0){
					loans.remove();
				}
			}
		}
	}
		
	
	//investment banks cycles through its outstanding loan payments and pays them, calls makeLoanPayment()
	/** This method causes an iBank to make monthly payments on all loans from its cBank.
	 * It cycles through loansFromCB, and accesses those objects. It gets the monthly payment due and attempts to remove that amount from its reserves.
	 * The money is transferred to the cBank via makeLoanPayment() which calls LoanFromCB.makePayment()
	 * If the full amount is not transferred, this will be caught in removeReserves()
	 * @throws Exception
	 */
	public void makeMonthlyPaymentsAllLoans() throws Exception{
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
					//appears to be handled in makeLoanPayment via removeReserves
					;
				}
			}
		}
		checkLoansForPaid();
	}
	
	//investment bank calculates how much the tick payment on a loan it should collect from a firm
	/** This method calculates how much an iBank should receive per month from a firm for a loan.
	 * @param balance Positive amount of loan balance.
	 * @return Monthly payment.
	 * @throws Exception 
	 */
	public double calculateFirmTickPayment(double balance) throws Exception{
		if (balance >= 0.0){
			double payment = balance/(1/loanRateFirms)/(1-(1/Math.pow((1+loanRateFirms),firmLoanYears)))/12;
			return payment;
		}
		else{
			throw new Exception("Cannot calculate monthly payment on negative balance!");
		}
	}
	
	//investment bank calculates how much it should pay commercial bank for potential loan
	/** This method calculates how much an iBank should pay per month to a cBank for a loan.
	 * @param balance Positive amount of loan balance.
	 * @return Monthly payment.
	 * @throws Exception 
	 */
	public double calculateCBTickPayment(double amount) throws Exception{
		if (amount >= 0.0){
			double payment = amount/(1/interestToCB)/(1-(1/Math.pow((1+interestToCB),cBLoanYears)))/12;
			return payment;
		}
		else{
			throw new Exception("Cannot calculate monthly payment on negative balance!");
		}
	}
	
	
	//investment banks pays back commercial bank for one loan
	/** This method is how an iBank makes an individual payment on a loan to a cBank.
	 * This method identifies the loanFromCB object and call its makePayment().
	 * This method is only used to make a monthly payment.
	 * To pay off full balance, call makeFullBalancePayment(). This is needed when a cBank calls collectFullLoans() or the iBank attempts to leave a cBank.
	 * In LoanFromCb.makePayment(), the money is transferred from the iBank to the cBank.
	 * removeReserves() is called in payBackAllLoans().
	 * @param tempId loanId.
	 * @param amount Positive amount to be paid by iBank.
	 * @return Returns the amount iBank actually paid. This is less than or equal to the intended amount.
	 * @throws Exception
	 */
	public double makeLoanPayment(String tempId, double amount) throws Exception{
		if (amount >= 0.0){
			if(loansFromCB.containsKey(tempId)){
				LoanFromCB thisLoan = loansFromCB.get(tempId);
				double paymentOutcome = thisLoan.makePayment(amount);
				if (paymentOutcome == -1.0){
					//	removeReserves(amount); this already happens
					removeLiabilities(amount);
					
					//destroy this loan by removing it from map
//					loansFromCB.remove(tempId); THIS NEEDS TO BE DONE ELSEWHERE
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
		else{
			throw new Exception("iBank cannot make a negative payment on a loan!");
		}
	}
	
	/** This method is how an iBank makes an individual full balance payment on a loan to a cBank.
	 * This method identifies the loanFromCB object and call its makePayment().
	 * This method is only used to make a full balance payment.
	 * This is needed when a cBank calls collectFullLoans() or the iBank attempts to leave a cBank.
	 * This method directly calls removeReserves().
	 * In LoanFromCB.makePayment(), the money is transferred from the iBank to the cBank.
	 * @param tempId loanId.
	 * @param amount Positive amount to be paid by iBank.
	 * @return Returns the amount iBank actually paid. This is less than or equal to the intended amount.
	 * @throws Exception
	 */
	public double makeFullBalancePayment(String tempId, double amount) throws Exception{
		if (amount >= 0.0){
			if(loansFromCB.containsKey(tempId)){
				LoanFromCB thisLoan = loansFromCB.get(tempId);
				double paymentOutcome = thisLoan.makePayment(amount);
				//The line below is the only difference between this method and makeLoanPayment().
				removeReserves(amount);
				if (paymentOutcome == -1.0){
					//	removeReserves(amount); this already happens
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
		else{
			throw new Exception("iBank cannot make a negative payment on a loan!");
		}
	}
	
	/** This method creates a loan from the iBank to a Firm.
	 * This loan is created whenever the iBank has enough reserves to cover the balance.
	 * If the iBank does not have enough reserves, it attempts to borrow the money from a cBank.
	 * It adds the loan to the iBank's waitingLoans which will be resolved by borrowWaitingLoans() and then resolveWaitingLoans().
	 * @param debtor Firm that wishes to borrow money.
	 * @param balance Positive amount Firm wishes to borrow.
	 * @param payment Positive amount that Firm will pay to iBank each month.
	 * @param loanId LoanId that is created by Firm.
	 * @return Returns true if successful.
	 * @throws Exception 
	 */
	public boolean createLoanFirm(Firm debtor, double balance, double payment, String loanId) throws Exception{
		if ((balance >= 0.0) && (payment >= 0.0)){
			//I may want to incorporate reserve requirement type thing later
			if (balance <= reserves){
				removeReserves(balance);
				addAssets(balance);
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
				return false;
			}
		}
		else{
			throw new Exception("Cannot create a negative loan!");
		}
	}
	
	
	//investment bank cycles through its wait list loans and calls requestLoanCB()
	/** This method allows an iBank to request funds from a cBank to make loans that it previously did not have the reserves for.
	 * @throws Exception
	 */
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
					requestLoanCB(thisLoan.getRemainingBalance(), thisLoan.getId());
				}
			}
		}		
	}
	
	//investment bank sees if it has borrowed enough money to lend to firms
	/** This method lets an iBank know if it was successful in procuring money for its waiting loans.
	 * If the iBank was able to borrow money from cBanks, it then creates as many corresponding loans to firms as it can (as it borrowed money for).
	 * It would not make sense for firms not receive money for loans in non sequential order as this model is set up.
	 * This method then removes all loans from the iBank's waiting list.
	 * @throws Exception
	 */
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
	
	/** This method removes all the loans from the iBank's waiting list.
	 * 
	 */
	public void removeAllWaitingLoans(){
		Collection<LoanToFirm> toDeleteLoanList = waitingLoans.values();
		Iterator<LoanToFirm> loansDelete = toDeleteLoanList.iterator();
		while (loansDelete.hasNext()){
			LoanToFirm thisLoan = loansDelete.next();
			loansDelete.remove();
		}
	}
	
	//investment bank checks to see if a loan passed by checking loanId. interfaces with firm
	/** This method determines whether or not an iBank created a specific loan to a firm.
	 * This method is called by firms to check if they received their loans or not.
	 * @param loanId loanId.
	 * @return Returns true if loan was made.
	 */
	public boolean checkLoanStatus(String loanId){
		if (loansToFirms.get(loanId) != null){
			return true;
		}
		else{
			return false;
		}
	}
	
	
	//investment bank receiving payment from firm
	/** This method is how iBanks receive loan payments from Firms. This method is only called by LoanFromFirm objects except in the event that an iBank identifies a delinquent loan.
	 * If an iBank identifies a delinquent loan, it calls this method to receive a payment of 0.0 and then destroys the loan.
	 * If the amount paid on the loan is not equal to the amount owed on the loan, the loan is delinquent and will be destroyed. 
	 * @param tempId Id of the loan that the payment is being made on.
	 * @param amount Positive amount that is being made on the loan.
	 * @return returns true if the full payment owed was received.
	 * @throws Exception Throws exception if any negative values.
	 */
	public boolean receivePayment(String tempId, double amount) throws Exception{
		if (amount >= 0.0){
			if(loansToFirms.containsKey(tempId)){
				LoanToFirm thisLoan = loansToFirms.get(tempId);
				double paymentOutcome = thisLoan.receivePayment(amount);
				if (paymentOutcome == -1.0){
					addReserves(amount);
					removeAssets(amount);
					//destroy this loan by removing it from map
//					loansToFirms.remove(tempId); THIS SHOULD HAPPEN ELSEWHERE
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
					addReserves(amount);
					removeAssets(amount);
					//now remove remaining loan balance from this bank's accounting
					double loss = thisLoan.getRemainingBalance();
					removeAssets(loss);
					//destroy this loan
					loansToFirms.remove(tempId);
					return false;
				}			
			}
			else{
				throw new Exception("investment bank should not be receiving this payment");
			}
		}
		else{
			throw new Exception("iBank cannot receive a negative payment from a firm!");
		}
	}
	
	//making sure firms have paid up all loans
	/** This method searches for any delinquent loans that an iBank owns. This method is in theory redundant because all loans should have payments made on them.
	 * If the payment made on a loan is less than the full amount, that loan should be removed in receivePayment().
	 * @throws Exception Throws Exception if any loan balance is negative.
	 */
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
					//destroy this loan
//					loansToFirms.remove(tempId);
					loans.remove();
				}
			}
		}		
	}
	
	/** This method removes all loans between the iBank and any cBanks.
	 * This method is only called when an iBank is going bankrupt.
	 * This method calls removeSingleLoan() for each individual loan.
	 * @throws Exception
	 */
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
	
	/** This method makes a fullBalancePayment() of 0 on the loan. This will register on the cBank side as the iBank defaulting on the loan.
	 * The cBank will then remove the loan from its side.
	 * @param thisLoan Loan to be defaulted on.
	 * @throws Exception
	 */
	public void removeSingleLoanFromCB(LoanFromCB thisLoan) throws Exception{
		//this condition is necessary if this method is called because a firm is going bankrupt
		//this may be unnecessary if I force each firm to default on its loans individually
		//when a firm defaults on its loans, I can make a payment of 0 which would remove this loan from this iBank already
		if(loansFromCB.containsValue(thisLoan)){
			String tempId = thisLoan.getId();
			makeFullBalancePayment(tempId, 0);
			loansFromCB.remove(tempId);
		}
		else{
			;
		}
	}
	
	/** This method removes all loans between the iBank and any Firms.
	 * This method is only called when an iBank is going bankrupt.
	 * In theory, this method is redundant since an iBank would already collectFullLoans() which would call Firm.makeFullBalancePayment() which would remove the loan from the firm end.
	 * collectFullLoans() would also call receivePayment() which would remove the loan from the iBank end.
	 * @throws Exception
	 */
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
	
	/** This method removes a specific loan between an iBank and a Firm.
	 * This method may be redundant as described in removeAllLoansToFirms().
	 * @param thisLoan Loan to be removed.
	 * @throws Exception
	 */
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
	
	/** This method allows an iBank to call in all of its outstanding loan balances to prevent this iBank from going bankrupt.
	 * This method is called whenever an iBank does not have enough reserves to make a loan payment to a cBank.
	 * @throws Exception Throws exception if Firm attempts to make a negative payment on a loan.
	 */
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
				double amountReceived = firm.makeFullBalancePayment(tempId, balance);
//				receivePayment(tempId, amountReceived); already called in above line
				loansToFirms.remove(tempId);
			}
		}
		if (reserves == -1.0){
			reserves = -2.0;
		}
	}
	
	
	public void iBankMoveTowards(GridPoint pt){
		//only move if we are not already in this grid location
		if (pt == null){
			//force consumers to move weird
			double probabilityX = rand.nextDouble() * 4;
			double probabilityY = rand.nextDouble() * 4;
			NdPoint myPoint = space.getLocation(this);
			NdPoint otherPoint = new NdPoint(myPoint.getX() + probabilityX, myPoint.getY() + probabilityY);
			double angle = SpatialMath.calcAngleFor2DMovement(space,  myPoint,  otherPoint);
			space.moveByVector(this, 1, angle, 0);
			myPoint = space.getLocation(this);
			grid.moveTo(this,  (int)myPoint.getX(), (int)myPoint.getY());
			
		}
		
		else if (!pt.equals(grid.getLocation(this))){
			NdPoint myPoint = space.getLocation(this);
			NdPoint otherPoint = new NdPoint(pt.getX(), pt.getY());
			double angle = SpatialMath.calcAngleFor2DMovement(space,  myPoint,  otherPoint);
			space.moveByVector(this, 1, angle, 0);
			myPoint = space.getLocation(this);
			grid.moveTo(this,  (int)myPoint.getX(), (int)myPoint.getY());
			
		}
		
		else{ /* if (pt.equals(grid.getLocation(this))){*/
			if (cBank == null){
				joinBank(identifyCBank());
			}
		}
		
		
	}
	
	public CommercialBank identifyCBank(){
		GridPoint pt = grid.getLocation(this);
		List<Object> comBanks = new ArrayList<Object>();
		for (Object obj : grid.getObjectsAt(pt.getX(), pt.getY())){
			if (obj instanceof CommercialBank){
				comBanks.add(obj);
			}
		}
		if (comBanks.size() > 0){
			int index = RandomHelper.nextIntFromTo(0, comBanks.size() - 1);
			Object obj = comBanks.get(index);
			CommercialBank toAdd = (CommercialBank) obj;
			return toAdd;
		}
			
//			NdPoint spacePt = space.getLocation(obj);
//			Context<Object> context = ContextUtils.getContext(obj);
//			context.remove(obj);
		return null;
		
	}
	
	public void iBankMove(){
		//get grid location of consumer
		GridPoint pt = grid.getLocation(this);
		//use GridCellNgh to create GridCells for the surrounding neighborhood
		GridCellNgh<CommercialBank> nghCreator = new GridCellNgh<CommercialBank>(grid, pt, CommercialBank.class, 1, 4);
		
		List<GridCell<CommercialBank>> gridCells = nghCreator.getNeighborhood(true);
		SimUtilities.shuffle(gridCells, RandomHelper.getUniform());
		
		GridPoint pointWithMostCBanks = null;
		if (cBank == null){
			int maxCount = -1;
			for (GridCell<CommercialBank> bank: gridCells){
				if (bank.size() > maxCount){
					pointWithMostCBanks = bank.getPoint();
					maxCount = bank.size();
				}
			}
		}
		iBankMoveTowards(pointWithMostCBanks);	
	}
	
	//This method removes the IBank from the simulation
	public void iBankDie(){
		Context<Object> context = ContextUtils.getContext(this);
		context.remove(this);
	}
	
	
	
	
	/** This scheduled basic method checks all of an iBank's loans to make sure they have been paid in full.
	 * This method should be unnecessary as payments on all loans will be made before this method is called.
	 * @throws Exception
	 */
	@ScheduledMethod(start = 7, interval = 13)
	public void invBank_getPayments_7() throws Exception{
		checkAllLoans();
	}

	/** This scheduled basic method forces an iBank to make monthly payments on all of its loans. This method should come after the iBank receives all monthly payments from Firms.
	 * This method will call collectFullLoans() if the iBank does not have enough reserves to make at least one of its payments to a cBank.
	 * @throws Exception
	 */
	@ScheduledMethod(start = 8, interval = 13)
	public void invBank_makePayments_8() throws Exception{
		makeMonthlyPaymentsAllLoans();
	}
	
	/** This scheduled basic method allows an iBank to attempt to borrow money to make loans to firms. This should come after Firms have decided whether or not to borrow from iBanks.
	 * @throws Exception
	 */
	@ScheduledMethod(start = 3, interval = 13)
	public void invBank_receiveRequests_3() throws Exception{
		iBankMove();
		borrowWaitingLoans();
	}
	
	//iBank lends out money to firms that were waiting
	/** This scheduled basic method immediately follows invBank_receiveRequests_2(). This method is where an iBank lends out any additional funds it was able to acquire to its waitingLoans.
	 * @throws Exception
	 */
	@ScheduledMethod(start = 4, interval = 13)
	public void invBank_borrowFunds_4() throws Exception{
		resolveWaitingLoans();
	}
	
	/** This scheduled status method determines if an iBank has gone bankrupt or not.
	 * If it has gone bankrupt, it has already called collectFullLoans() so all loans have been destroyed.
	 * It must now, probably redundantly, attempt to remove all outstanding relationships with other agents.
	 * If the iBank is not bankrupt, nothing happens. 
	 * @throws Exception
	 */
	@ScheduledMethod(start = 13, interval = 13)
	public void invBank_check_13() throws Exception{
		if (reserves <= -1){
			removeAllLoansFromCB();
			//this method should be redundant as collectFullLoans() should already have been called
			removeAllLoansToFirms();
			//this method should also be unnecessary given the timing of this scheduled method
			removeAllWaitingLoans();
			iBankDie();
		}
	}

}
