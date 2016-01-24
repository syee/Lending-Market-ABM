/**
 * 
 */
package model1;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
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
	//this value is 1.96 standard deviations from mean of 0.85
	private static final double FEAR_CUTOFF = 0.90268;
	private static final double FEAR_WITHDRAWAL_PROPORTION = 1.00;
	private static final int CUSTOMER_LEARN_COUNT = 10;
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	private double salary;
	private double cash;
	private double expenses;
	private CommercialBank cBank = null;
	private Random rand;
//	private double smallShockMult;
//	private double smallShockProb;
	private double largeShockMult;
	private double largeShockProb;
	private boolean shocked = false;
	private double consumptionDemand;
	private boolean isBankrupt;
	
	private NormalDistribution salaryCurve;
	private NormalDistribution consumptionCurve;	
	
	//proximity based learning stuff
	private double difference;
	private double net;
	private boolean panicFlag;
	private int panicCount; 
	private double othersConsumption;
	
	//illiquidity stuff
	private double shortTermPayout;
	private double longTermPayout;
	private double shortTermAssets;
	private double longTermAssets;
	private double totalAssets;
	
	
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
	public Consumer(ContinuousSpace<Object> space, Grid<Object> grid, double salary, double cash, double CONSUMER_DEVIATION_PERCENT, double consumptionMean, double largeShockMult, double largeShockProb, double shortTermPayout, double longTermPayout){
		this.space = space;
		this.grid = grid;
		this.cash = cash;
		this.largeShockMult = largeShockMult;
		this.largeShockProb = largeShockProb;
		this.isBankrupt = false;
		this.rand = new Random();
		this.consumptionDemand = 0;
		
		this.salaryCurve = new NormalDistribution(salary, CONSUMER_DEVIATION_PERCENT*salary);
//		this.salary = salaryCurve.sample();
		this.consumptionCurve = new NormalDistribution(consumptionMean, CONSUMER_DEVIATION_PERCENT*consumptionMean);
		initialConsumptionDemand();
//		this.expenses = consumptionDemand * salaryCurve.sample();
		this.difference = 0.0;
		this.net = 0.0;
		this.panicFlag = false;
		this.panicCount = 0;
		this.othersConsumption = 0.0;
		
		this.shortTermPayout = shortTermPayout;
		this.longTermPayout = longTermPayout;
		this.shortTermAssets = 0.0;
		this.longTermAssets = 0.0;
		this.totalAssets = cash;
	}
	
	/** This method samples the consumer's salary distribution to generate a salary for this month.
	 * 
	 */
	public double calculateSalary(){
		salary = salaryCurve.sample();
		return salary;
	}
	
	/** This method samples the consumer'jhdjhgdjkgdkdjhgsjkhgs expense distribution to generate expenses for this month.
	 * 
	 */
	public double calculateExpenses(){
		expenses = consumptionDemand * salary;
		return expenses;
	}
	
	/** This method uses a probability to determine if the consumer is suffering a small, large, or no shock this period.
	 * Compares probability to smallShockProb and largeShockProb.
	 * @return The cost of shocks in this month. Returns 0.0 if no shock occurred.
	 */
	public double calculateShock(){
		double probability = rand.nextDouble();
		if (probability <= largeShockProb){
			shocked = true;
			return largeShockMult * salary;
		}
//		else if (probability <= smallShockProb){
//			return smallShockMult * salary;
//		}
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
	
	public double getBankSavings() throws Exception{
		if (cBank == null){
			//this case should never happen
			return 0;
		}
		else{
			return cBank.returnConsumerBalance(this);
		}
	}
	
	public double getCash(){
		return cash;
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
				cash += withdrawal;
				if (withdrawal != amount){
					//add a method from creditor and pass it actualAmount so they get their money back before the consumer is destroyed
					//for example cBank.addAccount(savings(actualAmount), actualAMount)
					isBankrupt = true;
//					leaveBank(cBank);
				}
				//need to handle passing amount to creditor
				return withdrawal;
			}
			else{
				if (cash >= amount){
					//removeCash(amount);
					//need to handle passing amount to creditor
					return amount;
				}
				else{
					//removeCash(cash);
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
		System.out.println("I am " + this + " and I am trying to leave " + cBankDead);
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
	
	public void initialConsumptionDemand(){
		consumptionDemand = consumptionCurve.sample();
		while ((consumptionDemand > 1.0) || (consumptionDemand < 0.0)){
			consumptionDemand = consumptionCurve.sample();
		}
	}
	
	public double getConsumption(){
		return consumptionDemand;
	}
	
	public boolean getPanicFlag(){
		return panicFlag;
	}
	
	
	
	
	
	
	
	public double getTotalAssets(){
		return totalAssets;
	}
	
	public void transferMoney(){
		cash += longTermAssets * longTermPayout;
		longTermAssets = shortTermAssets;
		shortTermAssets = 0;
	}
	
	public double calculateTotalAssets(){
		totalAssets = cash + shortTermAssets + longTermAssets * shortTermPayout;
		return totalAssets;
	}
	
	
	public boolean getShockedStatus(){
		return shocked;
	}
	
	
	
	
	public void consumerProximityLearning(){
		System.out.println("HERE");
		GridPoint pt = grid.getLocation(this);
		GridCellNgh<Consumer> nghCreator = new GridCellNgh<Consumer>(grid, pt, Consumer.class, 50, 50);
		List<GridCell<Consumer>> gridCells = nghCreator.getNeighborhood(true);
		SimUtilities.shuffle(gridCells, RandomHelper.getUniform());
		int customerCount = 0;
		panicCount = 0;
		othersConsumption = 0.0;
		HashSet<Consumer> neighbors = new HashSet<Consumer>(10);
		for (GridCell<Consumer> location: gridCells){
			if (location.size() > 0){
				GridPoint otherPt = location.getPoint();
				for (Object obj : grid.getObjectsAt(otherPt.getX(), otherPt.getY())){
					//only look at the first 10 customers
					if (customerCount < CUSTOMER_LEARN_COUNT){
						if (obj instanceof Consumer){
							if (((Consumer) obj != this) && (neighbors.add((Consumer) obj))){
								if ((((Consumer) obj).getBank() != null) || ((Consumer) obj).getPanicFlag()){
									othersConsumption += ((Consumer) obj).getCash();
									if (((Consumer) obj).getPanicFlag()){
										panicCount++;
									}
									customerCount++;
								}
							}
							else{
								System.out.println("I already contain this consumer...why am I trying to add it twice");
							}
						}
					}
					else{
						break;
					}
				}
				
			}
		}
		System.out.println("I am " + this + " and I looked at");
		Iterator<Consumer> temps = neighbors.iterator();
		while (temps.hasNext()){
			Consumer thing = temps.next();
			System.out.println(thing);
			System.out.println(thing.getPanicFlag());
			
		}
		System.out.println("ASIDASDASD");
		if (customerCount > 0){

			
//			System.out.println(this + " I looked around and saw " + othersConsumption + " from this many consumers: " + customerCount);
//			System.out.println(this + " My consumption demand was " + consumptionDemand);
			//50% of the new consumptionDemand comes the previous period's consumptionDemand
//			double whiteNoise = rand.nextDouble() / 5 - 0.10;
//			consumptionDemand = consumptionDemand / 2 - whiteNoise;
//			
//			consumptionDemand = consumptionDemand / 2;
//			System.out.println(whiteNoise + " is white noise");
			//50% of the new consumptionDemand comes the consumptionDemands of the 10 nearest consumers in this period
			othersConsumption = othersConsumption / customerCount;
//			if (othersConsumption > FEAR_CUTOFF * salaryCurve.getMean()){
//				consumptionDemand = 1.0;
//			}
//			System.out.println(this + " My consumption demand is now " + consumptionDemand);
		}
	}
	
	/** This is the primary method of the second basic scheduled method to be called.
	 * This method calls calculateNet() to determine a consumer's net amount for a month.
	 * If the amount is negative, the consumer must withdraw money to cover the deficit or go bankrupt.
	 * If the full amount is not covered, the consumer's isBankrupt attribute is set to true.
	 * If the amount it positive, the consumer deposits the amount.
	 * If the consumer has a cBank and has enough savings to cover a deficit, the consumer considers his/her fear of a bank run.
	 * Based on the average consumption demands of the nearest 10 consumers from consumerProximityLearning(),
	 * The consumer makes a decision on whether or not to withdraw a portion of their remaining savings out of fear.
	 * @throws Exception
	 */
	public void discoverInitialNet() throws Exception{
		net = calculateNet();
//		System.out.println("HERERE");
//		System.out.println("My consumptionDemand is " + consumptionDemand);
//		if (consumptionDemand > FEAR_CUTOFF){
//			System.out.println(this + " is very scared");
//		}
		if (net < 0){
			double amountPaid = withdrawSavings(Math.abs(net));
			difference = net + amountPaid;
		}
		else{
			depositSavings(net);
		}
		
	}
	
	/** Part 2 of consumer paying out. Consumer looks around at neighbors wallets
	 * @throws Exception
	 */
	public void panicBasedConsumption() throws Exception{
		consumerProximityLearning();
		if (cBank != null){
			//Decision to withdraw is based on comparison of average consumption demands of 10 nearest consumers with FEAR_CUTOFF
			if ((othersConsumption >=500) || (panicCount > 2)){
				double remainingSavings = getBankSavings();
				//Consumer withdraws portion of their remaining savings after paying expenses. proportion currently 100%
				double fearWithdrawalAmount = remainingSavings * FEAR_WITHDRAWAL_PROPORTION;
				cash += withdrawSavings(fearWithdrawalAmount);
				leaveBank(cBank);
				panicFlag = true;
				System.out.println(this + " just made a complete fear withdrawal of " + remainingSavings + " because their othersConsumption was " + othersConsumption);
			}
			else{
				System.out.println(this + "did not make a fear withdrawal because my othersConsumption is " + othersConsumption);
			}
		}
	}
	
	/** Part 3 of consumer paying out. Consumer actually pays out money
	 * @throws Exception 
	 * 
	 */
	public void payingBills() throws Exception{
		if (net < 0){
			double amountPaid = Math.abs(net - difference);
			removeCash(amountPaid);
		}
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
			//force consumers to move randomly
			double targetX = rand.nextDouble() * 8;
			double targetY = rand.nextDouble() * 8;
			NdPoint myPoint = space.getLocation(this);
			NdPoint otherPoint = new NdPoint(myPoint.getX() + targetX, myPoint.getY() + targetY);
			double angleToMove = SpatialMath.calcAngleFor2DMovement(space,  myPoint,  otherPoint);
			int distanceToMove = 5;
			space.moveByVector(this, distanceToMove, angleToMove, 0);
			myPoint = space.getLocation(this);
			grid.moveTo(this,  (int)myPoint.getX(), (int)myPoint.getY());
			
		}
		else{
			double targetX = rand.nextDouble() * 4;
			double targetY = rand.nextDouble() * 4;
			NdPoint myPoint = space.getLocation(this);
			NdPoint otherPoint = new NdPoint(pt.getX() + targetX, pt.getY() + targetY);
			double angelToMove = SpatialMath.calcAngleFor2DMovement(space,  myPoint,  otherPoint);
			float distanceToMove = (float) Math.sqrt(Math.pow(otherPoint.getX() - myPoint.getX(), 2) +Math.pow(otherPoint.getY() - myPoint.getY(), 2));
			space.moveByVector(this, distanceToMove - 2, angelToMove, 0);
			myPoint = space.getLocation(this);
			grid.moveTo(this,  (int)myPoint.getX(), (int)myPoint.getY());
			
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
					System.out.println("I am " + this + " and I just joined " + toAdd);
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
				break;
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
		GridPoint pointWithMostCBanks = null;
		if(cBank == null){
			//use GridCellNgh to create GridCells for the surrounding neighborhood
			GridCellNgh<CommercialBank> nghCreator = new GridCellNgh<CommercialBank>(grid, pt, CommercialBank.class, 50, 50);
			
			List<GridCell<CommercialBank>> gridCells = nghCreator.getNeighborhood(true);
			SimUtilities.shuffle(gridCells, RandomHelper.getUniform());
			int maxCount = 0;
			for (GridCell<CommercialBank> bank: gridCells){
				if (bank.size() > maxCount){
					pointWithMostCBanks = bank.getPoint();
					maxCount = bank.size();
				}
			}
		}
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
	@ScheduledMethod(start = 1, interval = 16)
	public void consumer_tick_1() throws Exception{
		//This method should either go last or first of the basic scheduled methods.
		consumerMove();
		shocked = false;
//		discoverInitialNet();
//		panicBasedConsumption();
//		System.out.println("I am " + this + ". I made "+ net);
//		if (net < 0){
//			withdrawSavings(Math.abs(net));
//		}
//		else{
//			depositSavings(net);
//		}
//		System.out.println("I have this much " + getSavings() + " in my bank account!");
		
	}
	
	@ScheduledMethod(start = 2, interval = 16)
	public void consumer_tick_2() throws Exception{
		discoverInitialNet();
	}
	
	@ScheduledMethod(start = 3, interval = 16)
	public void consumer_tick_3() throws Exception{
		if (!shocked){
			panicBasedConsumption();
		}
		else{
			System.out.println("I am " + this + " and I was shocked :(");
		}
	}
	
	/** This is the first basic scheduled method to be called.
	 * The consumer initializes its consumption demand.
	 * The consumer than moves around to search for banks or just move
	 * This provides the basis of consumers passing information to each other. 
	 */
	@ScheduledMethod(start = 4, interval = 16)
	public void consumer_tick_4() throws Exception{
		//This method should either go last or first of the basic scheduled methods.
//		double net = calculateNet();
//		System.out.println("I am " + this + ". I made "+ net);
//		if (net < 0){
//			withdrawSavings(Math.abs(net));
//		}
//		else{
//			depositSavings(net);
//		}
//		System.out.println("I have this much " + getSavings() + " in my bank account!");
//		discoverInitialNet();
		payingBills();
		
	}
	
	//one of last scheduled methods
	/** This method forces a consumer to leave the environment if he or she is bankrupt.
	 * If a consumer is bankrupt, their account is removed from their cBank.
	 * If consumers reach a situation where isBankrupt is set to true, the contents of their bank accounts should have been withdrawn already.
	 * @throws Exception 
	 * 
	 */
	@ScheduledMethod(start = 14, interval = 16)
	public void consumer_check_14() throws Exception{
		if (isBankrupt){
			System.out.println("I am " + this + " and I just went bankrupt so I am about to leave my bank " + getBank());
			leaveBank(getBank());
			//consumer.die()
			consumerDie();
		}
		else{
			panicFlag = false;
		}
	}
	
	

}
