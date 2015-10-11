/**
 * 
 */
package model1;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.math3.distribution.NormalDistribution;

import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

/**
 * @author stevenyee
 *
 */
public class Firm {
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	private double revenue;
	private double expenses;
	private double reserves;
	private double loanPaymentTotal;
	private Random rand;
	private double smallShockMult;
	private double smallShockProb;
	private double largeShockMult;
	private double largeShockProb;
	private double corporateProfits;
	private boolean isUnpaid;
	
	//change this later to have multiple. list of investment banks
	private InvestmentBank iBank = null;
	private double firmLoanYears;
	private double loanRateFirms;
	
	private NormalDistribution revenueCurve;
	private NormalDistribution expenseCurve;
	private double FIRM_DEVIATION_PERCENT;
	private double PROFIT_REMOVAL;
	private double averageProfits;
	private HashMap<String, LoanFromIB> loansFromIB;
	private HashMap<String, LoanFromIB> waitingLoans;
	
	public Firm(ContinuousSpace<Object> space, Grid<Object> grid, double revenue, double expenses, double reserves, double FIRM_DEVIATION_PERCENT, double averageProfits, double smallShockMult, double largeShockMult, double smallShockProb, double largeShockProb, double loanRateFirms, double firmLoanYears, double PROFIT_REMOVAL){
		this.space = space;
		this.grid = grid;
		this.revenue = revenue;
		this.expenses = expenses;
		this.reserves = reserves;
		this.FIRM_DEVIATION_PERCENT = FIRM_DEVIATION_PERCENT;
		this.averageProfits = averageProfits;
		this.smallShockMult = smallShockMult;
		this.largeShockMult = largeShockMult;
		this.smallShockProb = smallShockProb;
		this.largeShockProb = largeShockProb;
		this.loanRateFirms = loanRateFirms;
		this.firmLoanYears = firmLoanYears;
		this.PROFIT_REMOVAL = PROFIT_REMOVAL;
		//should this be set to true or false initially?
		this.isUnpaid = false;
		
		this.revenueCurve = new NormalDistribution(revenue, FIRM_DEVIATION_PERCENT*revenue);
		this.revenue = revenueCurve.sample();
		this.expenseCurve = new NormalDistribution(revenue - averageProfits, FIRM_DEVIATION_PERCENT*(revenue - averageProfits));
		this.expenses = expenseCurve.sample();
		
		loansFromIB = new HashMap<String, LoanFromIB>();
		waitingLoans = new HashMap<String, LoanFromIB>();
		corporateProfits = 0;
		loanPaymentTotal = 0;
	}
	
	public void calculateRevenue(){
		revenue = revenueCurve.sample();
	}
	
	public void calculateExpenses(){
		expenses = expenseCurve.sample() + loanPaymentTotal;
	}
	
	public double calculateDisaster(){
		double probability = rand.nextDouble();
		if (probability <= largeShockProb){
			return largeShockMult * revenue;
		}
		else if (probability <= smallShockProb){
			return smallShockMult * revenue;
		}
		else{
			return 0.0;
		}
	}
	
	public double calculateNet(){
		double net = revenue - expenses - calculateDisaster();
		return net;
	}
	
	public void addReserves(double amount){
		reserves += amount;
	}
	
	public void removeCorporateProfits(){
		double removal = reserves * PROFIT_REMOVAL;
		removeReserves(removal);
		corporateProfits += removal;
	}
	
	public double removeReserves(double amount){
		if (reserves < amount){
			double toReturn = reserves;
			reserves = -1;
			return toReturn;
		}
		else{
			reserves -= amount;
			return amount;
		}
	}
	
	public void joinBank(InvestmentBank iBank){
//		if(cBank.addIBAccount(this)){
		if(this.iBank != null){
			this.iBank = iBank;
			//change this to allow more than one commercial bank in future
		}
		else{
			//already joined bank
		}
	}
	
	//firm calculates how much the tick payment would be for borrowing an amount
	public double calculateFirmTickPayment(double amount){
		double payment = amount/(1/loanRateFirms)/(1-(1/Math.pow((1+loanRateFirms),firmLoanYears)))/12;
		return payment;
	}
	
	
	//firm calculates maximum it can borrow based on its average profits for next 15 years
	public double calculateBalance(){
		double total = 0;
		for (double i = 0; i < firmLoanYears; i = i + 1.0){
			double timeValue = Math.pow((1 + loanRateFirms), i);
			double perYear = (averageProfits * 12)/(timeValue);
			total += perYear;
		}
		return total;		
	}
	
	//firm calculates if it can borrow, or if it goes bankrupt
	public boolean decisionBorrow(double amount){
		double payment = calculateFirmTickPayment(amount);
		return ((amount <= calculateBalance()) && (payment + loanPaymentTotal <= revenue));
	}
	
	
	//firm tries to borrow firm investment bank. balance would be equal to amount firm owes
	//logic could be refined
	public void askForLoan(double balance) throws Exception{
		if (iBank != null){
			if (decisionBorrow(balance)){
				//this adds the monthly payment to the firm's running tab of monthly loan payments
				//this may need to be adjusted later. This works if each firm only asks one investment bank for x dollars. Doesn't work if firm approaches multiple banks
				//this will likely be adjusted later. possibly divide it by number of investment banks
				loanPaymentTotal += calculateFirmTickPayment(balance);
				
				double tickPayment = calculateFirmTickPayment(balance);
				String newLoanId =  UUID.randomUUID().toString();
				LoanFromIB newLoan = new LoanFromIB(iBank, balance, tickPayment, newLoanId);
				if(iBank.createLoanFirm(this,  balance, tickPayment, newLoanId)){
					reserves += balance;
					loansFromIB.put(newLoanId, newLoan);
				}
				else{
					waitingLoans.put(newLoanId, newLoan);
				}
			}
			else{
				//firm goes bankrupt because it cannot meet its obligations
				;
			}	
		}
		else{
			//firm goes bankrupt since it cannot borrow money
		}
	}
	
	//firm sees if it received all of its necessary loans
	public boolean firmCheckLoanStatus(){
		Collection<LoanFromIB> loanList = waitingLoans.values();
		if (loanList != null){
			Iterator<LoanFromIB> loans = loanList.iterator();
			while (loans.hasNext()){
				LoanFromIB thisLoan = loans.next();
				//this actually would not have to be changed with multiple investment banks
				InvestmentBank creditor = thisLoan.getInvestmentBank();
				if (creditor.checkLoanStatus(thisLoan.getId())){
					;
				}
				else{
					//this means the loan did not go through and the firm must go bankrupt
					//all loans are assumed to be needed to prevent bankruptcy
					
					//I may want to adjust this situation later. I may want firms to have investment opportunities rather than only debt obligations
					return false;
				}
			}
		}
		return true;
	}
	
	//firm collects on all its loans (assumes firmCheckLoanStatus() has already been run successfully
	public void collectLoans(){
		Collection<LoanFromIB> loanList = waitingLoans.values();
		if (loanList != null){
			Iterator<LoanFromIB> loans = loanList.iterator();
			while (loans.hasNext()){
				LoanFromIB thisLoan = loans.next();
				loansFromIB.put(thisLoan.getId(), thisLoan);
				addReserves(thisLoan.getRemainingBalance());
				
				//the -1 I force reserves to once they reach bankruptcy???
				addReserves(1.0);
			}
			removeAllWaitingLoans();
		}
	}
	
	public void removeAllWaitingLoans(){
		Collection<LoanFromIB> toDeleteLoanList = waitingLoans.values();
		Iterator<LoanFromIB> loansDelete = toDeleteLoanList.iterator();
		while (loansDelete.hasNext()){
			LoanFromIB thisLoan = loansDelete.next();
			waitingLoans.remove(thisLoan.getId());
		}
	}
	
	
	//firm pays back investment bank for one loan
	public double makeLoanPayment(String tempId, double amount) throws Exception{
		if (amount >= 0.0){
			if(loansFromIB.containsKey(tempId)){
				LoanFromIB thisLoan = loansFromIB.get(tempId);
				double paymentOutcome = thisLoan.makePayment(amount);
				if (paymentOutcome == -1.0){
					//	removeReserves(amount); this already happens
					//destroy this loan by removing it from map
					loansFromIB.remove(tempId);
					return amount;
				}
				else if (thisLoan.getPayment() == paymentOutcome){
					//full payment made
					return amount;
				}
				else{
					//less than full payment made
					//should I add a default counter?
					//now remove remaining loan balance from this firm's accounting
					//destroy this firm
					///
					///
					// THIS FIRM NEEDS TO BE DESTROYED
					//
					//
					////
					loansFromIB.remove(tempId);
					return paymentOutcome;
				}
			}
			else{
				throw new Exception("Investment Bank should not be making this payment to this Commercial Bank ");
			}
		}
		else{
			throw new Exception("Firm cannot make a negative loan payment!");
		}
	}
	
	//firm pays back investment bank for one loan
	public double makeFullBalancePayment(String tempId, double amount) throws Exception{
		if (amount >= 0.0){
			if(loansFromIB.containsKey(tempId)){
				LoanFromIB thisLoan = loansFromIB.get(tempId);
				double paymentOutcome = thisLoan.makePayment(amount);
				removeReserves(amount);
				if (paymentOutcome == -1.0){
					//	removeReserves(amount); this already happens
					//destroy this loan by removing it from map
					loansFromIB.remove(tempId);
					return amount;
				}
				else if (thisLoan.getPayment() == paymentOutcome){
					//full payment made
					return amount;
				}
				else{
					//less than full payment made
					//should I add a default counter?
					//now remove remaining loan balance from this firm's accounting
					//destroy this firm
					///
					///
					// THIS FIRM NEEDS TO BE DESTROYED
					//
					//
					////
					loansFromIB.remove(tempId);
					return paymentOutcome;
				}
			}
			else{
				throw new Exception("Investment Bank should not be making this payment to this Commercial Bank ");
			}
		}
		else{
			throw new Exception("Firm cannot make a negative loan payment!");
		}
	}
	
	//firm cycles through its outstanding loan payments and pays them, calls makeLoanPayment()
	public void payBackAllLoans() throws Exception{
		Collection<LoanFromIB> loanList = loansFromIB.values();
		if (loanList != null){
			Iterator<LoanFromIB> loans = loanList.iterator();
			while (loans.hasNext()){
				LoanFromIB thisLoan = loans.next();
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
	
	public void firms_balanceTick_1(){
		calculateRevenue();
		calculateExpenses();
		double net = calculateNet();
		if(iBank == null){
			//search for a iBank
			//joinBank()
		}
		if (net < 0){
			if (reserves >= Math.abs(net)){
				removeReserves(Math.abs(net));
				isUnpaid = false;
			}
			else{
				if (iBank != null){
					askForLoan(net);
					if (reserves >= Math.abs(net)){
						removeReserves(Math.abs(net));
						isUnpaid = false;
					}
					else{
						isUnpaid = true;
					}
				}
			}
		}
		else if (net >= 0){
			isUnpaid = false;
			//removeCorporateProfits();
		}
	}
	
	//firm sees if it's waiting list loans have all passed
	public void firms_waitingStatus_44() throws Exception{
		if (isUnpaid){
			if(firmCheckLoanStatus()){
				//If firmCheckLoanStatus() returns true, the firm should not go bankrupt this tick
				collectLoans();
				double net = calculateNet();
				if (reserves >= Math.abs(net)){
					removeReserves(Math.abs(net));
					isUnpaid = false;
				}
				else{
					//firm goes bankrupt
					payBackAllLoans();
					removeAllWaitingLoans();
				}
			}
			else{
				//firm goes bankrupt
				//this will cause firm to default on all loans it owes.
				payBackAllLoans();
				removeAllWaitingLoans();
			}
		}
	}
	
	public void firms_makePayments_5() throws Exception{
		payBackAllLoans();
	}
	
	
	
}
