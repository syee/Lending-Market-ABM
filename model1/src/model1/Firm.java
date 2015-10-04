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
	private Random rand;
	private double smallShockMult;
	private double smallShockProb;
	private double largeShockMult;
	private double largeShockProb;
	private double corporateProfits;
	
	//change this later to have multiple. list of investment banks
	private InvestmentBank iBank = null;
	private double firmLoanYears;
	private double loanRateFirms;
	
	private NormalDistribution revenueCurve;
	private NormalDistribution expenseCurve;
	private double FIRM_DEVIATION_PERCENT;
	private double averageProfits;
	private HashMap<String, LoanFromIB> loansFromIB;
	private HashMap<String, LoanFromIB> waitingLoans;
	
	public Firm(ContinuousSpace<Object> space, Grid<Object> grid, double revenue, double expenses, double reserves, double FIRM_DEVIATION_PERCENT, double averageProfits, double smallShockMult, double largeShockMult, double smallShockProb, double largeShockProb, double loanRateFirms, double firmLoanYears){
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
		
		this.revenueCurve = new NormalDistribution(revenue, FIRM_DEVIATION_PERCENT*revenue);
		this.revenue = revenueCurve.sample();
		this.expenseCurve = new NormalDistribution(revenue - averageProfits, FIRM_DEVIATION_PERCENT*(revenue - averageProfits));
		this.expenses = expenseCurve.sample();
		
		loansFromIB = new HashMap<String, LoanFromIB>();
		waitingLoans = new HashMap<String, LoanFromIB>();
		corporateProfits = 0;
	}
	
	public void calculateRevenue(){
		revenue = revenueCurve.sample();
	}
	
	public void calculateExpenses(){
		expenses = expenseCurve.sample();
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
		double removal = reserves * 0.5;
		reserves -= removal;
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
			this.iBank = iBank;
			//change this to allow more than one commercial bank in future
//		}
//		else{
			//already joined bank
//		}
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
		return (amount <= calculateBalance());
	}
	
	
	//firm tries to borrow firm investment bank. balance would be equal to amount firm owes
	//logic could be refined
	public void createLoan(double balance){
		if (decisionBorrow(balance)){
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
	
	//firm sees if it received all of its necessary loans
	public boolean firmCheckLoanStatus(){
		Collection<LoanFromIB> loanList = waitingLoans.values();
		if (loanList != null){
			Iterator<LoanFromIB> loans = loanList.iterator();
			while (loans.hasNext()){
				LoanFromIB thisLoan = loans.next();
				InvestmentBank creditor = thisLoan.getInvestmentBank();
				if (creditor.checkLoanStatus(thisLoan.getId())){
					;
				}
				else{
					//this means the loan did not go through and the firm must go bankrupt
					//all loans are assumed to be needed to prevent bankruptcy
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
		}
	}
	
	
	//firm pays back investment bank for one loan
		public boolean makeLoanPayment(String tempId, double amount) throws Exception{
			if(loansFromIB.containsKey(tempId)){
				LoanFromIB thisLoan = loansFromIB.get(tempId);
				double paymentOutcome = thisLoan.makePayment(amount);
				if (paymentOutcome == -1.0){
					//	removeReserves(amount); this already happens
					removeReserves(amount);
					//destroy this loan by removing it from map
					loansFromIB.remove(tempId);
					return true;
				}
				else if (thisLoan.getPayment() == paymentOutcome){
					//full payment made
					removeReserves(amount);
					return true;
				}
				else{
					//less than full payment made
					//should I add a default counter?
					removeReserves(amount);
					//now remove remaining loan balance from this firm's accounting
					//destroy this firm
					///
					///
					// THIS FIRM NEEDS TO BE DESTROYED
					//
					//
					////
					loansFromIB.remove(tempId);
					return false;
				}
			}
			else{
				throw new Exception("Investment Bank should not be making this payment to this Commercial Bank ");
			}
		}
		
		//firm cycles through its outstanding loan payments and pays them, calls makeLoanPayment()
		public void makeLoanPayments() throws Exception{
			Collection<LoanFromIB> loanList = loansFromIB.values();
			if (loanList != null){
				Iterator<LoanFromIB> loans = loanList.iterator();
				while (loans.hasNext()){
					LoanFromIB thisLoan = loans.next();
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
}
