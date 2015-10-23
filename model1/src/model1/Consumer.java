/**
 * 
 */
package model1;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.distribution.NormalDistribution;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.SpatialMath;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.graph.Network;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;
import repast.simphony.util.SimUtilities;

/**
 * @author stevenyee
 *
 */
public class Consumer {
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	private double salary;
	private double cash;
	private double expenses;
	private CommercialBank cBank = null;
	private Random rand;
	private double smallShockMult;
	private double smallShockProb;
	private double largeShockMult;
	private double largeShockProb;
	private boolean isBankrupt;
	
	private NormalDistribution salaryCurve;
	private NormalDistribution expenseCurve;
	private double CONSUMER_DEVIATION_PERCENT;
	private double averageSavings;
	
	
	/** This method instantiates a Consumer Object.
	 * @param space
	 * @param grid
	 * @param salary Default salary base.
	 * @param cash Default cash amount.
	 * @param CONSUMER_DEVIATION_PERCENT Standard deviation in consumer income and expenses.
	 * @param averageSavings Net positive difference between salary and expenses.
	 * @param smallShockMult Magnitude of a small shock.
	 * @param largeShockMult Magnitude of a large shock.
	 * @param smallShockProb Probability of a small shock.
	 * @param largeShockProb Probability of a large shock.
	 */
	public Consumer(ContinuousSpace<Object> space, Grid<Object> grid, double salary, double cash, double CONSUMER_DEVIATION_PERCENT, double averageSavings, double smallShockMult, double largeShockMult, double smallShockProb, double largeShockProb){
		this.space = space;
		this.grid = grid;
		this.cash = cash;
		this.CONSUMER_DEVIATION_PERCENT = CONSUMER_DEVIATION_PERCENT;
		this.averageSavings = averageSavings;
		this.smallShockMult = smallShockMult;
		this.largeShockMult = largeShockMult;
		this.smallShockProb = smallShockProb;
		this.largeShockProb = largeShockProb;
		this.isBankrupt = false;
		this.rand = new Random();
		
		this.salaryCurve = new NormalDistribution(salary, CONSUMER_DEVIATION_PERCENT*salary);
		this.salary = salaryCurve.sample();
		this.expenseCurve = new NormalDistribution(salary - averageSavings, CONSUMER_DEVIATION_PERCENT*(salary - averageSavings));
		this.expenses = expenseCurve.sample();
	}
	
	/** This method samples the consumer's salary distribution to generate a salary for this month.
	 * 
	 */
	public double calculateSalary(){
		salary = salaryCurve.sample();
		return salary;
	}
	
	/** This method samples the consumer's expense distribution to generate expenses for this month.
	 * 
	 */
	public double calculateExpenses(){
		expenses = expenseCurve.sample();
		return expenses;
	}
	
	/** This method uses a probability to determine if the consumer is suffering a small, large, or no shock this period.
	 * Compares probability to smallShockProb and largeShockProb.
	 * @return The cost of shocks in this month. Returns 0.0 if no shock occurred.
	 */
	public double calculateShock(){
		double probability = rand.nextDouble();
		if (probability <= largeShockProb){
			return largeShockMult * salary;
		}
		else if (probability <= smallShockProb){
			return smallShockMult * salary;
		}
		else{
			return 0;
		}
	}
	
	/** This method calculates a consumer's net savings for a month. It subtracts expenses and shocks from a consumer's salary.
	 * This method calls calculateSalary(), calculateExpenses(), and calculateShock().
	 * @return The consumer's net savings for a month.
	 */
	public double calculateNet(){
		double net = calculateSalary() - calculateExpenses() - calculateShock();
		return net;
	}
	
	/** This method deposits a positive amount into a consumer's cBank account or his/her cash if the consumer has no cBank account.
	 * @param amount This is the amount the consumer is depositing.
	 * @throws Exception Throws exception if amount is less than 0.
	 */
	public void depositSavings(double amount) throws Exception{
		if (amount >= 0.0){
			if (cBank != null){
				cBank.deposit(this, amount);
			}
			else{
				//This is the case where the consumer has no bank and stores his savings under his mattress
				addCash(amount);
			}
		}
		else{
			throw new Exception("Consumer cannot deposit a negative amount!");
		}
	}
	
	public double getSavings() throws Exception{
		if (cBank == null){
			return cash;
		}
		else{
			return cBank.returnConsumerBalance(this);
		}
	}
	
	
	//if consumer was looking at paying this back toward a loan, I would want to make sure cBank or whomever received their money before the consumer was deleted
	//somewhere else, there will need to be a check to make sure creditor received the full amount they expected
	/** This method withdraws money from a consumer's cBank account or his/her cash if the consumer has no cBank account.
	 * If the consumer does not have enough savings to withdraw the full amount, the consumer is assumed to go bankrupt.
	 * This method does not return amount to a creditor. Consumers currently have no actual creditors (10/10).
	 * @param amount Positive amount the consumer needs to pay off monthly deficit.
	 * @throws Exception Throws exception if amount is less than 0.
	 */
	public double withdrawSavings(double amount) throws Exception{
		if (amount >= 0){
			if (cBank != null){
				double withdrawal = cBank.withdraw(this, amount);
				if (withdrawal != amount){
					//add a method from creditor and pass it actualAmount so they get their money back before the consumer is destroyed
					//for example cBank.addAccount(savings(actualAmount), actualAMount)
					isBankrupt = true;
					leaveBank(cBank);
				}
				//need to handle passing amount to creditor
				return withdrawal;
			}
			else{
				if (cash >= amount){
					removeCash(amount);
					//need to handle passing amount to creditor
					return amount;
				}
				else{
					removeCash(cash);
					//remove consumer//add a method from creditor and pass it actualAmount so they get their money back before the consumer is destroyed
					//for example cBank.addAccount(savings(actualAmount), actualAMount)
					isBankrupt = true;
					return cash;
				}
			}
		}
		else{
			throw new Exception("Consumer cannot withdraw a negative amount!");
		}
	}
	
	//Before this method is called, you must check to make sure the consumer does not already have a banking relationship with cBank
	/** This method creates an account for a consumer at a cBank assuming no such account already exists.
	 * Consumers are assumed to only have one cBank so they deposit all their cash into this bank.
	 * @param cBankNew This is the cBank the consumer wishes to create an account at.
	 * @return returns true if the consumer successfully created an account.
	 * @throws Exception Throws exception if consumer has a negative cash pile value.
	 */
	public boolean joinBank(CommercialBank cBankNew) throws Exception{
		//I may want to eventually switch this to searching a list of the consumer's cBanks. This assumes each consumer has only one cBank
		if (cBankNew == null){
			return false;
		}
		
		if (this.cBank == null){
			cBankNew.addAccount(this, cash);
			removeCash(cash);
			this.cBank = cBankNew;
			return true;
		}
		else{
			//Consumer already has a cBank so it doesn't need another one?
			//May change this later to indicate consumer already has account at this cBank
			return false;
		}
	}
	
	/** This method allows consumers to leave their cBank. A consumer is assumed to only leave their bank should the bank go bankrupt.
	 * Hence consumers recover no money from their account. Calls cBank.removeAccount(). CONSUMERS LEAVING ACCOUNTS HANDLED HERE.
	 * @param cBankDead This parameter is currently unnecessary as consumers only have a single cBank.
	 * @return returns true if cBankDead matches consumer's account so consumer leaves the bank.
	 * @throws Exception 
	 */
	public boolean leaveBank(CommercialBank cBankDead) throws Exception{
		//I may want to eventually switch this to searching a list of the consumer's cBanks. This assumes each consumer has only one cBank
		if (cBank == null){
			return false;
		}
		
		if (this.cBank == cBankDead){
			cBank.removeAccount(this);
			cBank = null;
			return true;
		}
		else{
			return false;
		}
	}
	
	public boolean forcedToLeaveBank(CommercialBank cBankDead) throws Exception{
		//I may want to eventually switch this to searching a list of the consumer's cBanks. This assumes each consumer has only one cBank
		cBank = null;
		return true;
	}
	
	/** This method returns a consumer's cBank. This assumes consumer's only have one cBank
	 * @return returns a consumer's cBank. null is returned if no cBank relationship exists.
	 */
	public CommercialBank getBank(){
		return cBank;
	}
	
	/** This method is called when a consumer has a net positive amount for a month, but does not have a cBank account.
	 * @param amount This is the positive amount a consumer wishes to add to their cash pile.
	 * @throws Exception  Throws exception if amount is less than 0.
	 */
	public void addCash(double amount) throws Exception{
		if (amount >= 0){
			cash += amount;
		}
		else{
			throw new Exception("Consumer cannot add a negative cash amount!");
		}
	}
	
	/** This method is called when a consumer needs to tap into their savings to pay off a monthly deficit but the consumer does not have a cBank account.
	 * @param amount The positive amount a consumer wishes to remove from their cash pile.
	 * @throws Exception Throws exception if consumer tries to remove a negative amount.
	 */
	public void removeCash(double amount) throws Exception{
		if (amount >= 0){
			cash -= amount;
		}
		else{
			throw new Exception("Consumer cannot remove a negative cash amount!");
		}
	}
	
	
	
	public void consumerMoveTowards(GridPoint pt) throws Exception{
		//only move if we are not already in this grid location
		if (pt == null){
			//force consumers to move weird
			double probabilityX = rand.nextDouble() * 8;
			double probabilityY = rand.nextDouble() * 8;
			NdPoint myPoint = space.getLocation(this);
			NdPoint otherPoint = new NdPoint(myPoint.getX() + probabilityX, myPoint.getY() + probabilityY);
			double angle = SpatialMath.calcAngleFor2DMovement(space,  myPoint,  otherPoint);
			space.moveByVector(this, 5, angle, 0);
			myPoint = space.getLocation(this);
			grid.moveTo(this,  (int)myPoint.getX(), (int)myPoint.getY());
			
		}
		
		else{ /* (!pt.equals(grid.getLocation(this))){*/
			double probabilityX = rand.nextDouble() * 4;
			double probabilityY = rand.nextDouble() * 4;
			NdPoint myPoint = space.getLocation(this);
			NdPoint otherPoint = new NdPoint(pt.getX() + probabilityX, pt.getY() + probabilityY);
			double angle = SpatialMath.calcAngleFor2DMovement(space,  myPoint,  otherPoint);
			float dist = (float) Math.sqrt(Math.pow(otherPoint.getX() - myPoint.getX(), 2) +Math.pow(otherPoint.getY() - myPoint.getY(), 2));
			
			
			space.moveByVector(this, dist - 2, angle, 0);
			myPoint = space.getLocation(this);
			grid.moveTo(this,  (int)myPoint.getX(), (int)myPoint.getY());
			
			if (cBank == null){
				CommercialBank toAdd = identifyCBank(pt);
//				System.out.println(this);
//				System.out.println(toAdd);
				if (toAdd != null){
//					System.out.println("HERE");
					if (joinBank(toAdd)){
//						System.out.println("THERE");
						Context<Object> context = ContextUtils.getContext(this);
//						System.out.println(context);
						Network<Object> net = (Network<Object>) context.getProjection("consumers_cBanks network");
//						System.out.println(net);
						net.addEdge(this, toAdd);
					}
				}
			}
		}
	}
	
	public CommercialBank identifyCBank(GridPoint pt){
//		GridPoint pt = grid.getLocation(this);
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

		return null;
	}
	
	public void consumerMove() throws Exception{
		//get grid location of consumer
		GridPoint pt = grid.getLocation(this);
		//use GridCellNgh to create GridCells for the surrounding neighborhood
		GridCellNgh<CommercialBank> nghCreator = new GridCellNgh<CommercialBank>(grid, pt, CommercialBank.class, 50, 50);
		
		List<GridCell<CommercialBank>> gridCells = nghCreator.getNeighborhood(true);
		SimUtilities.shuffle(gridCells, RandomHelper.getUniform());
		
		GridPoint pointWithMostCBanks = null;
//		if (cBank == null){
			int maxCount = 0;
			for (GridCell<CommercialBank> bank: gridCells){
				if (bank.size() > maxCount){
					pointWithMostCBanks = bank.getPoint();
					maxCount = bank.size();
				}
			}
//		}
//		System.out.println("I am consumer " + this + ". I found the point with the most banks at "+ pointWithMostCBanks);
		consumerMoveTowards(pointWithMostCBanks);	
	}
	
	//this method removes the bankrupt consumer from the model
	public void consumerDie(){
		Context<Object> context = ContextUtils.getContext(this);
		context.remove(this);
	}
	
	
	/** This is the first basic scheduled method to be called.
	 * This method calls calculateNet() to determine a consumer's net amount for a month.
	 * If the amount is negative, the consumer must withdraw money to cover the deficit or go bankrupt.
	 * If the full amount is not covered, the consumer's isBankrupt attribute is set to true.
	 * If the amount is positive, the consumer deposits the amount.
	 * The consumer also searches for a cBank if they do not already have one.
	 * @throws Exception Withdrawal and Deposit amounts must be positive.
	 * 
	 */
	@ScheduledMethod(start = 1, interval = 13)
	public void consumer_tick_1() throws Exception{
		//This method should either go last or first of the basic scheduled methods.
		consumerMove();
		double net = calculateNet();
		System.out.println("I am " + this + ". I made "+ net);
		if (net < 0){
			withdrawSavings(Math.abs(net));
		}
		else{
			depositSavings(net);
		}
		System.out.println("I have this much " + getSavings() + " in my bank account!");
		
	}
	
	//one of last scheduled methods
	/** This method forces a consumer to leave the environment if he or she is bankrupt.
	 * If a consumer is bankrupt, their account is removed from their cBank.
	 * If consumers reach a situation where isBankrupt is set to true, the contents of their bank accounts should have been withdrawn already.
	 * @throws Exception 
	 * 
	 */
	@ScheduledMethod(start = 11, interval = 13)
	public void consumer_check_11() throws Exception{
		if (isBankrupt){
			leaveBank(getBank());
			//consumer.die()
			consumerDie();
		}
	}
	
	

}
