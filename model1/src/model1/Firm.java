/**
 * 
 */
package model1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.math3.distribution.NormalDistribution;

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
	private double net;
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
	
	/** This method instantiates a Firm object
	 * @param space
	 * @param grid
	 * @param revenue
	 * @param reserves
	 * @param FIRM_DEVIATION_PERCENT
	 * @param averageProfits
	 * @param smallShockMult
	 * @param largeShockMult
	 * @param smallShockProb
	 * @param largeShockProb
	 * @param loanRateFirms
	 * @param firmLoanYears
	 * @param PROFIT_REMOVAL
	 */
	public Firm(ContinuousSpace<Object> space, Grid<Object> grid, double revenue, double reserves, double FIRM_DEVIATION_PERCENT, double averageProfits, double smallShockMult, double largeShockMult, double smallShockProb, double largeShockProb, double loanRateFirms, double firmLoanYears, double PROFIT_REMOVAL){
		this.space = space;
		this.grid = grid;
		this.revenue = revenue;
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
		this.net = 0.0;
		this.rand = new Random();
		
		this.revenueCurve = new NormalDistribution(revenue, FIRM_DEVIATION_PERCENT*revenue);
		this.revenue = revenueCurve.sample();
		this.expenseCurve = new NormalDistribution(revenue - averageProfits, FIRM_DEVIATION_PERCENT*(revenue - averageProfits));
		this.expenses = expenseCurve.sample();
		
		loansFromIB = new HashMap<String, LoanFromIB>();
		waitingLoans = new HashMap<String, LoanFromIB>();
		corporateProfits = 0;
		loanPaymentTotal = 0;
	}
	
	/** This method samples the Firm's revenue distribution to generate revenue for this month.
	 * 
	 */
	public double calculateRevenue(){
		revenue = revenueCurve.sample();
		return revenue;
	}
	
	/** This method samples the Firm's expense distribution to generate expenses for this month.
	 * 
	 */
	public double calculateExpenses(){
		expenses = expenseCurve.sample() + loanPaymentTotal;
		return expenses;
	}
	
	/** This method uses a probability to determine if the Firm is suffering a small, large, or no shock this period.
	 * Compares probability to smallShockProb and largeShockProb.
	 * @return The cost of shocks in this month. Returns 0.0 if no shock occurred.
	 */
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
	
	/** This method calculates a Firm's net savings for a month. It subtracts expenses and shocks from a Firm's revenue.
	 * This method calls calculateRevenue(), calculateExpenses(), and calculateShock().
	 * @return The Firm's net savings for a month.
	 */
	public double calculateNet(){
		double net = calculateRevenue() - calculateExpenses() - calculateDisaster();
		return net;
	}
	
	/** This method adds a positive amount to a Firm's reserves.
	 * @param amount Positive amount to be added.
	 * @throws Exception Throws exception if amount is less than 0.
	 */
	public void addReserves(double amount) throws Exception{
		if (amount >= 0.0){
			reserves += amount;
		}
		else{
			throw new Exception("Firm cannot add a negative value to its reserves!");
		}
	}
	
//	public void removeCorporateProfits(){
//		double removal = reserves * PROFIT_REMOVAL;
//		removeReserves(removal);
//		corporateProfits += removal;
//	}
	
	
	/** This method removes a positive amount from a Firm's reserves.
	 * If the Firm does not have enough reserves, it returns its full reserves. Its reserves are set to -1.0 to indicate the Firm has gone bankrupt.
	 * The Firm attempts to borrow money in firms_balanceTick_1(), not here.
	 * @param amount Positive amount the Firm must produce from its reserves.
	 * @return Minimum of desired amount and Firm reserves.
	 * @throws Exception Throws exception if amount is less than 0.
	 */
	public double removeReserves(double amount) throws Exception{
		if (amount >= 0.0){
			if (reserves < amount){
				double toReturn = reserves;
				reserves = -1.0;
				return toReturn;
			}
			else{
				reserves -= amount;
				return amount;
			}
		}
		else{
			throw new Exception("Firm cannot remove a negative amount from its reserves!");
		}
	}
	
	/** This method allows a Firm to create a relationship with an iBank.
	 * @param iBank The iBank the Firm wishes to join.
	 * @return Returns true if no previous relationship between this Firm and iBank existed.
	 */
	public boolean joinBank(InvestmentBank iBank){
//		if(cBank.addIBAccount(this)){
		if (iBank == null){
			return false;
		}
		if(this.iBank != null){
			this.iBank = iBank;
			return true;
			//change this to allow more than one commercial bank in future
		}
		else{
			//already joined bank
			return false;
		}
	}
	
	/** This method allows a Firm to leave an iBank.
	 * The Firm must first pay off all of its loans to that iBank to sever the relationship.
	 * @param iBankDone iBank the Firm wishes to leave.
	 * @return Returns true if successful.
	 * @throws Exception
	 */
	public boolean leaveBank(InvestmentBank iBankDone) throws Exception{
		if (iBankDone == null){
			return false;
		}
		
		if (iBank == iBankDone){
			//pay off any loans to the bank
			Collection<LoanFromIB> loanList = loansFromIB.values();
			if (loanList != null){
				Iterator<LoanFromIB> loans = loanList.iterator();
				while (loans.hasNext()){
					LoanFromIB thisLoan = loans.next();
					String tempId = thisLoan.getId();
					if (thisLoan.getBank() == iBankDone){
						double balance = thisLoan.getRemainingBalance();
						makeFullBalancePayment(tempId, balance);
						loansFromIB.remove(thisLoan);
					}
				}
			}
			//this works for a single cBank per iBank
			iBank = null;
			return true;
		}
		else{
			return false;	
		}
	}
	
	//firm calculates how much the tick payment would be for borrowing an amount
	/** This method calculates how much a Firm should pay per month to an iBank for a loan.
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
			throw new Exception("Firm cannot calculate payment for a negative balance!");
		}
	}
	
	
//	//firm calculates maximum it can borrow based on its average profits for next 15 years
//	/** This method calculates how much a Firm could borrow based on its average profits for the next FirmLoanYaers.
//	 * @return
//	 */
//	public double calculateBalance(){
//		double total = 0;
//		for (double i = 0; i < firmLoanYears; i = i + 1.0){
//			double timeValue = Math.pow((1 + loanRateFirms), i);
//			double perYear = (averageProfits * 12)/(timeValue);
//			total += perYear;
//		}
//		return total;		
//	}
	
	//firm calculates if it can borrow, or if it goes bankrupt
	/** This method calculates whether or not a Firm is able to borrow a certain balance.
	 * This compares the Firm's existing monthly loan payments to the maximum amount it can afford based on its average profits.
	 * @param balance Positive amount the Firm wishes to borrow.
	 * @return Returns true if Firm can afford to borrow the balance.
	 * @throws Exception
	 */
	public boolean decisionBorrow(double balance) throws Exception{
		if (balance >= 0.0){
			double payment = calculateFirmTickPayment(balance);
			return (payment + loanPaymentTotal <= averageProfits);
		}
		else{
			throw new Exception("Firm cannot decide to borrow a negative amount!");
		}
	}
	
	
	//firm tries to borrow firm investment bank. balance would be equal to amount firm owes
	//logic could be refined
	/** This method allows a Firm to ask for a loan from an iBank.
	 * If the iBank has the reserves to make the loan, the Firm receives the loan. Otherwise the loan is added to the waitingLoans for both the Firm and iBank.
	 * @param balance Positive amount the Firm wishes to borrow.
	 * @throws Exception
	 */
	public void askForLoan(double balance) throws Exception{
		if (balance >= 0.0){
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
						addReserves(balance);
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
				;
			}
		}
		else{
			throw new Exception("Firm cannot ask to borrow a negative amount!");
		}
	}
	
	//firm sees if it received all of its necessary loans
	/** This method allows a Firm to check if all of its waiting loans have been approved.
	 * If all loans in question register with the correct iBanks, the Firm  will receive the balances of the loans in a scheduled method which calls collectLoans().
	 * @return Return true if Firm receives all of its waitingLoans.
	 */
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
					//reserves are added in collectLoans()
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
	/** This method allows a Firm to collect on all of its approved waitingLoans balances. This method is only executed assuming firmCheckLoanStatus() returns true.
	 * All waitingLoans are then removed.
	 * @throws Exception 
	 * 
	 */
	public void collectLoans() throws Exception{
		Collection<LoanFromIB> loanList = waitingLoans.values();
		if (loanList != null){
			Iterator<LoanFromIB> loans = loanList.iterator();
			while (loans.hasNext()){
				LoanFromIB thisLoan = loans.next();
				loansFromIB.put(thisLoan.getId(), thisLoan);
				addReserves(thisLoan.getRemainingBalance());
			}
			removeAllWaitingLoans();
		}
	}
	
	/** This method removes all the waitingLoans from a Firm.
	 * 
	 */
	public void removeAllWaitingLoans(){
		Collection<LoanFromIB> toDeleteLoanList = waitingLoans.values();
		Iterator<LoanFromIB> loansDelete = toDeleteLoanList.iterator();
		while (loansDelete.hasNext()){
			LoanFromIB thisLoan = loansDelete.next();
			waitingLoans.remove(thisLoan.getId());
		}
	}
	
	
	//firm pays back investment bank for one loan
	/** This method is how a Firm makes an individual payment on a loan to an iBank.
	 * This method identifies the loanFromIB object and call its makePayment().
	 * This method is only used to make a monthly payment.
	 * To pay off full balance, call makeFullBalancePayment(). This is needed when an iBank calls collectFullLoans() or the Firm attempts to leave the iBank.
	 * In LoanFromIb.makePayment(), the money is transferred from the Firm to the iBank.
	 * removeReserves() is called in makeMonthlyPaymentsAllLoans().
	 * @param tempId loanId.
	 * @param amount Positive amount to be paid by Firm.
	 * @return Returns the amount Firm actually paid. This is less than or equal to the intended amount.
	 * @throws Exception
	 */
	public double makeLoanPayment(String tempId, double amount) throws Exception{
		if (amount >= 0.0){
			if(loansFromIB.containsKey(tempId)){
				LoanFromIB thisLoan = loansFromIB.get(tempId);
				double paymentOutcome = thisLoan.makePayment(amount);
				if (paymentOutcome == -1.0){
					//	removeReserves(amount); this already happens
					//destroy this loan by removing it from map
					loanPaymentTotal -= thisLoan.getPayment();
//					loansFromIB.remove(tempId); DO THIS ELSEWHERE
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
					loanPaymentTotal -= thisLoan.getPayment();
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
	
	//firm pays back investment bank full remaining balance for one loan
	/** This method is how a Firm makes an individual full balance payment on a loan to an iBank.
	 * This method identifies the loanFromIB object and call its makePayment().
	 * This method is only used to make a full balance payment.
	 * This is needed when an iBank calls collectFullLoans() or the Firm attempts to leave the iBank.
	 * This method directly calls removeReserves().
	 * In LoanFromIB.makePayment(), the money is transferred from the Firm to the iBank.
	 * @param tempId loanId.
	 * @param amount Positive amount to be paid by Firm.
	 * @return Returns the amount Firm actually paid. This is less than or equal to the intended amount.
	 * @throws Exception
	 */
	public double makeFullBalancePayment(String tempId, double amount) throws Exception{
		if (amount >= 0.0){
			if(loansFromIB.containsKey(tempId)){
				LoanFromIB thisLoan = loansFromIB.get(tempId);
				double paymentOutcome = thisLoan.makePayment(amount);
				removeReserves(amount);
				if (paymentOutcome == -1.0){
					//	removeReserves(amount); this already happens
					//destroy this loan by removing it from map
					loanPaymentTotal -= thisLoan.getPayment();
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
					loanPaymentTotal -= thisLoan.getPayment();
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
	
	//this method removes all loans that have been paid off
	public void checkLoansForPaid(){
		Collection<LoanFromIB> loanList = loansFromIB.values();
		if (loanList != null){
			Iterator<LoanFromIB> loans = loanList.iterator();
			while (loans.hasNext()){
				LoanFromIB thisLoan = loans.next();
				if (thisLoan.getRemainingBalance() == 0){
					loans.remove();
				}
			}
		}
	}
	
	//firm cycles through its outstanding loan payments and pays them, calls makeLoanPayment()
	/** This method causes a Firm to make monthly payments on all loans from its iBank.
	 * It cycles through loansFromIB, and accesses those objects. It gets the monthly payment due and attempts to remove that amount from its reserves.
	 * The money is transferred to the iBank via makeLoanPayment() which calls LoanFromIB.makePayment()
	 * If the full amount is not transferred, this will be caught in removeReserves()
	 * @throws Exception
	 */
	public void makeMonthlyPaymentsAllLoans() throws Exception{
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
	
	
	
	public void firmMoveTowards(GridPoint pt){
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
		
		else{ /* (pt.equals(grid.getLocation(this))){*/
			if (iBank == null){
				joinBank(identifyIBank());
			}
		}
	}
	
	public InvestmentBank identifyIBank(){
		GridPoint pt = grid.getLocation(this);
		List<Object> invBanks = new ArrayList<Object>();
		for (Object obj : grid.getObjectsAt(pt.getX(), pt.getY())){
			if (obj instanceof InvestmentBank){
				invBanks.add(obj);
			}
		}
		if (invBanks.size() > 0){
			int index = RandomHelper.nextIntFromTo(0, invBanks.size() - 1);
			Object obj = invBanks.get(index);
			InvestmentBank toAdd = (InvestmentBank) obj;
			return toAdd;
		}
			
//			NdPoint spacePt = space.getLocation(obj);
//			Context<Object> context = ContextUtils.getContext(obj);
//			context.remove(obj);
		return null;
		
	}
	
	public void firmMove(){
		//get grid location of consumer
		GridPoint pt = grid.getLocation(this);
		//use GridCellNgh to create GridCells for the surrounding neighborhood
		GridCellNgh<InvestmentBank> nghCreator = new GridCellNgh<InvestmentBank>(grid, pt, InvestmentBank.class, 1, 10);
		
		List<GridCell<InvestmentBank>> gridCells = nghCreator.getNeighborhood(true);
		SimUtilities.shuffle(gridCells, RandomHelper.getUniform());
		
		GridPoint pointWithMostIBanks = null;
//		if (iBank == null){
			int maxCount = -1;
			for (GridCell<InvestmentBank> bank: gridCells){
				if (bank.size() > maxCount){
					pointWithMostIBanks = bank.getPoint();
					maxCount = bank.size();
				}
			}
//		}
		firmMoveTowards(pointWithMostIBanks);
	}
	
	public void firmDie(){
		Context<Object> context = ContextUtils.getContext(this);
		context.remove(this);
	}
	
	
	
	/** This scheduled basic method is the first to be called.
	 * This method calculates a Firm's net amount for a month. If the amount is negative, the firm attempts to pay the deficit out of its reserves.
	 * If the firm does not have enough reserves, it attempts to borrow the remaining amount from its iBank.
	 * If the Firm now has enough in reserves to cover the remanining balance, it will do so. Otherwise the Firm's loan request is pending and will be resolved in firms_waitingStatus_4()
	 * @throws Exception
	 */
	@ScheduledMethod(start = 2, interval = 13)
	public void firms_balanceTick_2() throws Exception{
		net = calculateNet() - loanPaymentTotal;
		firmMove();
		if (net < 0){
			if (reserves >= Math.abs(net)){
				removeReserves(Math.abs(net));
				isUnpaid = false;
			}
			else{
				if (iBank != null){
					net = net + reserves;
					removeReserves(reserves);
					askForLoan(Math.abs(net));
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
			addReserves(net);
			//removeCorporateProfits();
		}
	}
	
	//firm sees if it's waiting list loans have all passed
	/** This scheduled basic method is called after iBanks have received (invBank_receiveRequests_2()) and processed requests for loans (invBank_borrowFunds_3()).
	 * If firms did not pay off their monthly balance in the above scheduled method, they check to see if their loan requests were approved.
	 * If all the requests were approved, the Firm will collect on those loans and attempt to pay off its monthly balance and any loan payments.
	 * If all requests were not approved, I am assuming the Firm will go bankrupt since it requested the exact amount it needed to not go bankrupt.
	 * If not all approved, the Firm will still attempt to collect on any Loans that were approved. This could be a problem but I do not have a way of resolving it later. Ideally, iBanks only make their loans assuming all other iBanks will make the loans.
	 * @throws Exception
	 */
	@ScheduledMethod(start = 5, interval = 13)
	public void firms_waitingStatus_5() throws Exception{
		if (isUnpaid){
			if(firmCheckLoanStatus()){
				//If firmCheckLoanStatus() returns true, the firm should not go bankrupt this tick
				collectLoans();
				if (reserves >= Math.abs(net)){
					removeReserves(Math.abs(net));
					isUnpaid = false;
					makeMonthlyPaymentsAllLoans();
					removeAllWaitingLoans();
				}
				else{
					//firm goes bankrupt
					removeReserves(reserves); // I am assuming monthly deficit it senior to long term debt
					makeMonthlyPaymentsAllLoans();
					removeAllWaitingLoans();
					firmDie();
				}
			}
			else{
				collectLoans(); // I am assuming monthly deficit is senior to long term debt
				removeReserves(reserves);
				//firm goes bankrupt
				//this will cause firm to default on all loans it owes.
				makeMonthlyPaymentsAllLoans();
				removeAllWaitingLoans();
			}
		}
	}
	
	/** This scheduled basic method immediately follows the above scheduled method.
	 * In this method, Firms that had net positive months will now make all their monthly payments.
	 * @throws Exception
	 */
	@ScheduledMethod(start = 6, interval = 13)
	public void firms_makePayments_6() throws Exception{
		if (!(isUnpaid)){
			makeMonthlyPaymentsAllLoans();
		}
	}
	
	@ScheduledMethod(start = 10, interval = 13)
	public void firms_check_10() throws Exception{
		if (reserves <= -1.0){
			//firm has gone bankrupt
			leaveBank(iBank);
			removeAllWaitingLoans();
			firmDie();
		}
		else{
			//cycle starts again
			isUnpaid = true;
		}
	}
	
	
	
}
