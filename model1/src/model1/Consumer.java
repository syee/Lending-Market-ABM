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
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;
import repast.simphony.util.SimUtilities;

/**
 * @author stevenyee
 *
 */
public class Consumer {
	
	
	private int CUSTOMER_LEARN_COUNT;
		
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	private int age = 0;
	private double salary;
	private double initialEndowment;
	private double cash;
	private double expenses;
	private CommercialBank cBank = null;
	private Random rand;

	private double largeShockMult;
	private double largeShockProb;
	private boolean shocked = false;
	private double consumptionDemand;
	private boolean isBankrupt;
	
	private NormalDistribution salaryCurve;
	private NormalDistribution consumptionCurve;	
	
	//proximity based learning stuff
//	private double difference;
	private double net;
	private double trueLiquidityDemand;
	private boolean panicFlag;
	private boolean liquidityPanic;
	private boolean panicPanic;
	private boolean purePanic;
	private boolean shockedButStanding;
	
	private int consumerPBCount;
	private int neighborPanicCount;
	private int neighborShockCount;
	private double neighborPanicProportion;
	private double estimatedPanicWithdrawal;
	private int myPanicCount;
	private double othersConsumption;
	private RepastEdge<Object> bankEdge = null;
	
	//illiquidity stuff
	private double shortTermPayout;
	private double longTermPayout;
	private double shortTermAssets;
	private double longTermAssets;
	private double totalAssets;
	private double currentBankAssetsEstimate = 0.0;
	
	private DiamondDybvig DD;
	private int placeInLine;
	private boolean unnecessaryPanic;
	private double bankShortTermPayout;
	private double bankLongTermPayout;
	private double bankCost2;
	private boolean allConsumersVisible;
	private boolean bankVisible;
	
	private double probExpectedWithdrawal;
	
	private double assetsFutureP;
	private double expectedWithdrawalsP;
	
	private double assetsFutureL;
	private double expectedWithdrawalsL;
	
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
	public Consumer(ContinuousSpace<Object> space, Grid<Object> grid, double salary, double shortTermEndowment, double CONSUMER_DEVIATION_PERCENT, double consumptionMean, double largeShockMult, double largeShockProb, double shortTermPayout, double longTermPayout, int CUSTOMER_LEARN_COUNT, DiamondDybvig DD, double bankShortTermPayout, double bankLongTermPayout, double bankCost2, double averageWithdrawal, boolean allConsumersVisible, boolean bankVisible){
		this.space = space;
		this.grid = grid;
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
//		this.difference = 0.0;
		this.net = 0.0;
		this.panicFlag = false;
		this.liquidityPanic = false;
		this.panicPanic = false;
		this.neighborPanicCount = 0;
		this.myPanicCount = 0;
		this.othersConsumption = 0.0;
		this.CUSTOMER_LEARN_COUNT = CUSTOMER_LEARN_COUNT;
		
		this.shortTermPayout = shortTermPayout;
		this.longTermPayout = longTermPayout;
		this.initialEndowment = shortTermEndowment;
		this.shortTermAssets = shortTermEndowment;
		this.longTermAssets = shortTermEndowment / longTermPayout;
		this.totalAssets = cash;
		
		this.DD = DD;
		this.bankShortTermPayout = bankShortTermPayout;
		this.bankLongTermPayout = bankLongTermPayout;
		this.bankCost2 = bankCost2;
		this.allConsumersVisible = allConsumersVisible;
		this.bankVisible = bankVisible;
		
		this.probExpectedWithdrawal = averageWithdrawal;
		calculateBankAssets();
	}
	
	/** This method samples the consumer's salary distribution to generate a salary for this month.
	 * 
	 */
	public double calculateSalary(){
		salary = salaryCurve.sample();
		while (salary < 0){
			salary = salaryCurve.sample();
		}
		return salary;
	}
	
	
	/** This method samples the consumer's expense distribution to generate expenses for this month.
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
		else{
			return 0;
		}
	}
	
	/** This method calculates a consumer's net savings for a month. It subtracts expenses and shocks from a consumer's salary.
	 * This method calls calculateSalary(), calculateExpenses(), and calculateShock().
	 * @return The consumer's net savings for a month.
	 */
	public void calculateNet(){
		net = calculateSalary() - calculateExpenses() - calculateShock();
		trueLiquidityDemand = net;
	}
	
	/** This method deposits a positive amount into a consumer's cBank account or his/her cash if the consumer has no cBank account.
	 * @param amount This is the amount the consumer is depositing.
	 * @throws Exception Throws exception if amount is less than 0.
	 */
	public void depositSavings(double amount) throws Exception{
		if (amount >= -1.0){
			if (cBank != null){
				shortTermAssets += amount;
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
	
		
	public double getTrueLiquidityDemand(){
		return trueLiquidityDemand;
	}
	
	public double getLongTermAssets(){
		return longTermAssets;
	}
	
	public double getShortTermAssets(){
		return shortTermAssets;
	}
	
	public void calculateBankAssets(){
		currentBankAssetsEstimate =  shortTermAssets * DD.getConsumerCount() + longTermAssets * DD.getConsumerCount() * bankShortTermPayout;
	}
	
	public double getCurrentBankAssetsEstimate(){
		return currentBankAssetsEstimate;
	}
	
	public double getTotalAssets(){
		totalAssets = longTermAssets * shortTermPayout + shortTermAssets + cash;
		return totalAssets;
	}
	
	public double withdrawLongTerm(double desiredAmount) throws Exception{
		double longTermHeld = getLongTermAssets() * shortTermPayout;
		if (desiredAmount > longTermHeld){
			cBank.consumerWithdrawLongTerm(this, longTermHeld);
			longTermAssets = 0;
			getTotalAssets();
			return longTermHeld;
		}
		else{
			longTermAssets -= desiredAmount / shortTermPayout;
			getTotalAssets();
			cBank.consumerWithdrawLongTerm(this, desiredAmount);
			return desiredAmount;
		}
	}
	
	public double withdrawShortTerm(double desiredAmount) throws Exception{
		double shortTermHeld = shortTermAssets;
		if (desiredAmount > shortTermHeld){
			cBank.consumerWithdrawShortTerm(this, shortTermHeld);
			shortTermAssets = 0;
			getTotalAssets();
			return shortTermHeld;
		}
		else{
			shortTermAssets -= desiredAmount;
			getTotalAssets();
			cBank.consumerWithdrawShortTerm(this, desiredAmount);
			return desiredAmount;
		}
	}
	
	//This method happens at end of consumer tick
	public void transferMoney(){
		double temp = longTermAssets * longTermPayout;
		longTermAssets = shortTermAssets;
		shortTermAssets = temp;
		getTotalAssets();
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
	public double withdrawSavings(double originalAmount) throws Exception{
		double leftOver = originalAmount;
//		System.out.println(this + " My leftover is " + leftOver);
		if (leftOver >= -0.2){
			if (cBank != null){
				if (leftOver <= cash){
//					System.out.println("My satisfactory cash amount is " + cash);
					cash -= leftOver;
					getTotalAssets();
					return leftOver;
				}
				else{
					leftOver -= cash;
//					System.out.println("My not enough cash amount is " + cash);
					cash = 0;
//					System.out.println("The first leftover amount is " + leftOver);
					leftOver -= withdrawShortTerm(leftOver);
//					System.out.println("After using my shortTerm funds, I owe " + leftOver);
					leftOver -= withdrawLongTerm(leftOver);
//					System.out.println("After using my longTerm funds, I owe " + leftOver);
					if (leftOver <= 0.2){
						getTotalAssets();
						shockedButStanding = true;
						return originalAmount;
					}
					else{
						isBankrupt = true;
						getTotalAssets();
//						System.out.println("I went bankrupt. My assets are now " + totalAssets);
//						System.out.println("I still owe " + leftOver);
//						System.out.println("FINISHED " + this);
						return originalAmount - leftOver;
					}
				}
			}
			else{
				throw new Exception("ERROR from :" + this);
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
			cBankNew.addAccount(this, (cash + shortTermAssets), longTermAssets);
			shortTermAssets += cash;
			removeCash(cash);
			getTotalAssets();
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
//		System.out.println("I am " + this + " and I am trying to leave " + cBankDead);
		//I may want to eventually switch this to searching a list of the consumer's cBanks. This assumes each consumer has only one cBank
		if (cBank == null){
			return false;
		}
		
		if (this.cBank == cBankDead){
			cBank.consumerWithdrawShortTerm(this, shortTermAssets);
			cBank.consumerWithdrawLongTerm(this, longTermAssets * shortTermPayout);
			cBank.removeAccount(this);
			cBank = null;
			double temp = getTotalAssets();
			shortTermAssets = 0;
			longTermAssets = 0;
			cash = temp;
			getTotalAssets();
			
			Context<Object> context = ContextUtils.getContext(this);
			Network<Object> net = (Network<Object>) context.getProjection("consumers_cBanks network");
			net.removeEdge(bankEdge);
			
			return true;
		}
		else{
			return false;
		}
	}
	
	public boolean forcedToLeaveBank(CommercialBank cBankDead) throws Exception{
		//I may want to eventually switch this to searching a list of the consumer's cBanks. This assumes each consumer has only one cBank
		cBank = null;
		shortTermAssets = 0;
		longTermAssets = 0;
		getTotalAssets();
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
	
	public boolean getPurePanic(){
		return purePanic;
	}
	
	public boolean getPartialShock(){
		return shockedButStanding;
	}
	
	public int getConsumerPBCount(){
		return consumerPBCount;
	}
	
	public boolean getPanicPanic(){
		return panicPanic;
	}
	
	public boolean getLiquidityPanic(){
		return liquidityPanic;
	}
	
	public boolean getUnnecessary(){
		return unnecessaryPanic;
	}
	
	
	public double getOthersConsumption(){
		return othersConsumption;
	}
	
	public boolean getShockedStatus(){
		return shocked;
	}
	
	public int getNeighborPanicCount(){
		return neighborPanicCount;
	}
	
	public int getMyPanicCount(){
		return myPanicCount;
	}
	
	public double getEstimatedPanicWithdrawal(){
		return estimatedPanicWithdrawal;
	}
	
	public double getNeighborPanicProportion(){
		return neighborPanicProportion;
	}


	public double getAssetsFutureP(){
		return assetsFutureP;
	}
	
	public double getExpectedWithdrawalsP(){
		return expectedWithdrawalsP;
	}
	
	public double getAssetsFutureL(){
		return assetsFutureL;
	}
	
	public double getExpectedWithdrawalsL(){
		return expectedWithdrawalsL;
	}
	
	
	public void consumerProximityLearning(){
		GridPoint pt = grid.getLocation(this);
		GridCellNgh<Consumer> nghCreator = new GridCellNgh<Consumer>(grid, pt, Consumer.class, CUSTOMER_LEARN_COUNT, CUSTOMER_LEARN_COUNT);
		List<GridCell<Consumer>> gridCells = nghCreator.getNeighborhood(true);
		SimUtilities.shuffle(gridCells, RandomHelper.getUniform());
		HashSet<Consumer> neighbors = new HashSet<Consumer>(10);
		for (GridCell<Consumer> location: gridCells){
			if (location.size() > 0){
				GridPoint otherPt = location.getPoint();
				for (Object obj : grid.getObjectsAt(otherPt.getX(), otherPt.getY())){
					//only look at the first 10 customers
					if (consumerPBCount < CUSTOMER_LEARN_COUNT){
						if (obj instanceof Consumer){
							if ((((Consumer) obj).getBank() != null) || ((Consumer) obj).getPanicFlag()){
								if (((Consumer) obj != this) && (neighbors.add((Consumer) obj))){
									othersConsumption -= ((Consumer) obj).getTrueLiquidityDemand();
//									System.out.println("I am " + this);
									if (((Consumer) obj).getPanicFlag()){
										neighborPanicCount++;
									}
									consumerPBCount++;
								}
							}
							else{
//								System.out.println("I already contain this consumer...why am I trying to add it twice");
							}
						}
					}
					else{
						break;
					}
				}
				
			}
		}

		if (consumerPBCount > 0){
			othersConsumption = othersConsumption / consumerPBCount * DD.getConsumerCount();
		}
		else{
			othersConsumption = cash + 0.0001;
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
		calculateNet();
		if (net < 0){
			if (cBank != null){
				DD.addInitialWithdrawals(net);
				cash = withdrawSavings(Math.abs(net));
			}
			else{
				;
			}
		}
		else{
			if (cBank != null){
				DD.addInitialWithdrawals(net);
			}
			depositSavings(net);
		}
	}
	
	
	public boolean liquidityCheckerP(double othersConsumption1, double estimatedBankShortTermAssets, double estimatedBankLongTermAssets){
		double estimatedWithdrawals = othersConsumption1;
		double tempBankShort = estimatedBankShortTermAssets;
		double tempBankLong = estimatedBankLongTermAssets;
		
		if (estimatedWithdrawals > tempBankShort){
			estimatedWithdrawals -= tempBankShort;
			tempBankShort = 0.0;
			if (estimatedWithdrawals <= tempBankLong){
				tempBankLong -= estimatedWithdrawals;
				estimatedWithdrawals = 0.0;
			}
			else{
				estimatedWithdrawals -= tempBankLong;
				tempBankLong = 0.0;
			}
		}else{
			tempBankShort -= estimatedWithdrawals;
			estimatedWithdrawals = 0.0;
		}
		assetsFutureP = bankShortTermPayout * tempBankShort + bankLongTermPayout * tempBankLong - bankCost2;		
		expectedWithdrawalsP = probExpectedWithdrawal * DD.getConsumerCount();
		
		return (assetsFutureP >= expectedWithdrawalsP);		
	}
	
	public boolean liquidityCheckerL(double othersConsumption1, double estimatedBankShortTermAssets, double estimatedBankLongTermAssets){
		double estimatedWithdrawals = othersConsumption1;
		double tempBankShort = estimatedBankShortTermAssets;
		double tempBankLong = estimatedBankLongTermAssets;
		
		if (estimatedWithdrawals > tempBankShort){
			estimatedWithdrawals -= tempBankShort;
			tempBankShort = 0.0;
			if (estimatedWithdrawals <= tempBankLong){
				tempBankLong -= estimatedWithdrawals;
				estimatedWithdrawals = 0.0;
			}
			else{
				estimatedWithdrawals -= tempBankLong;
				tempBankLong = 0.0;
			}
		}else{
			tempBankShort -= estimatedWithdrawals;
			estimatedWithdrawals = 0.0;
		}
		assetsFutureL = bankShortTermPayout * tempBankShort + bankLongTermPayout * tempBankLong - bankCost2;		
		expectedWithdrawalsL = probExpectedWithdrawal * DD.getConsumerCount();
		
		return (assetsFutureL >= expectedWithdrawalsL);		
	}
	
	
	/** Part 2 of consumer paying out. Consumer looks around at neighbors wallets
	 * @throws Exception
	 */
	public void panicBasedConsumption() throws Exception{
		neighborPanicProportion = 0.00001;
		if (cBank != null){
			consumerProximityLearning();
			DD.addPlaceInLine();
			placeInLine = DD.getPlaceInLine();
						

			if ((!allConsumersVisible) && (!bankVisible)){
				if (consumerPBCount == 0){
					estimatedPanicWithdrawal = 0.0001;
					neighborPanicProportion = 0.00001;
				}
				else{
					estimatedPanicWithdrawal = (shortTermAssets + shortTermPayout * longTermAssets) * neighborPanicCount / consumerPBCount * DD.getConsumerCount();
					neighborPanicProportion = neighborPanicCount / (double) consumerPBCount;
				}
				if (neighborPanicCount > 0){
					DD.addPanicEstimateCount();
					DD.addPanicsEstimate(estimatedPanicWithdrawal);
				}
				if (neighborPanicCount > 0){
					if (!liquidityCheckerP(estimatedPanicWithdrawal, shortTermAssets * DD.getConsumerCount(), longTermAssets * DD.getConsumerCount())){
							panicPanic = true;
							panicFlag = true;
					}
					if (!liquidityCheckerL(othersConsumption, shortTermAssets * DD.getConsumerCount(), longTermAssets * DD.getConsumerCount())){
						liquidityPanic = true;
						panicFlag = true;
					}
				}
				else{
					estimatedPanicWithdrawal = 0.0001;
					if (!liquidityCheckerL(othersConsumption, shortTermAssets * DD.getConsumerCount(), longTermAssets * DD.getConsumerCount())){
						liquidityPanic = true;
						panicFlag = true;
					}
				}
			}
			else if ((allConsumersVisible) && (!bankVisible)){
				if (consumerPBCount == 0){
					estimatedPanicWithdrawal = 0.0001;
					neighborPanicProportion = 0.00001;
				}
				else{
					estimatedPanicWithdrawal = (shortTermAssets + shortTermPayout * longTermAssets) * neighborPanicCount / consumerPBCount * DD.getConsumerCount();
					neighborPanicProportion = neighborPanicCount / (double) consumerPBCount;
				}
				if (liquidityCheckerP(estimatedPanicWithdrawal, shortTermAssets * DD.getConsumerCount(), longTermAssets * DD.getConsumerCount())){
					panicPanic = true;
					panicFlag = true;
				}
				if (liquidityCheckerL(DD.getInitialWithdrawals(), shortTermAssets * DD.getConsumerCount(), longTermAssets * DD.getConsumerCount())){
					liquidityPanic = true;
					panicFlag = true;
				}
			}
			else if ((!allConsumersVisible) && (bankVisible)){
				if (consumerPBCount == 0){
					estimatedPanicWithdrawal = 0.0001;
					neighborPanicProportion = 0.00001;
				}
				else{
					estimatedPanicWithdrawal = (shortTermAssets + shortTermPayout * longTermAssets) * neighborPanicCount / consumerPBCount * DD.getConsumerCount();
					neighborPanicProportion = neighborPanicCount / (double) consumerPBCount;
				}
				if (!liquidityCheckerP(estimatedPanicWithdrawal, DD.getBankShort(), DD.getBankLong())){
					panicPanic = true;
					panicFlag = true;
				}
				if (!liquidityCheckerL(othersConsumption, DD.getBankShort(), DD.getBankLong())){
					liquidityPanic = true;
					panicFlag = true;
				}
			}
			else{
				if (consumerPBCount == 0){
					estimatedPanicWithdrawal = 0.0001;
					neighborPanicProportion = 0.00001;
				}
				else{
					estimatedPanicWithdrawal = (shortTermAssets + shortTermPayout * longTermAssets) * neighborPanicCount / consumerPBCount * DD.getConsumerCount();
					neighborPanicProportion = neighborPanicCount / (double) consumerPBCount;
				}
				if (!liquidityCheckerP(estimatedPanicWithdrawal * DD.getConsumerCount(), DD.getBankShort(), DD.getBankLong())){
					panicPanic = true;
					panicFlag = true;
				}
				if (!liquidityCheckerL(DD.getInitialWithdrawals(), DD.getBankShort(), DD.getBankLong())){
					liquidityPanic = true;
					panicFlag = true;
				}
			}
			

			if (panicFlag){
				unnecessaryPanic = DD.getUnnecessary();	
				double fearWithdrawalAmount = getTotalAssets();
				purePanic = (panicFlag && (shockedButStanding || !shocked));
				cash = withdrawSavings(fearWithdrawalAmount);
				leaveBank(cBank);
				myPanicCount++;
			}
		}
	}
	
	/** Part 3 of consumer paying out. Consumer actually pays out money
	 * @throws Exception 
	 * 
	 */
	public void payingBills() throws Exception{
		if (net < 0){
			removeCash(Math.abs(net));
			getTotalAssets();
		}
	}
	
	
	
	/** This method is called when a consumer has a net positive amount for a month, but does not have a cBank account.
	 * @param amount This is the positive amount a consumer wishes to add to their cash pile.
	 * @throws Exception  Throws exception if amount is less than 0.
	 */
	public void addCash(double amount) throws Exception{
		if (amount >= 0){
			cash += amount;
			getLongTermAssets();
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
			if (cash >= amount){
				cash -= amount;
			}
			else{
				cash = 0;
				isBankrupt = true;
			}
		}
		else{
			throw new Exception("Consumer cannot remove a negative cash amount!");
		}
	}
	
	public int getAge(){
		return age;
	}
	
	public int getPlaceInLine(){
		return placeInLine;
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
			double targetX = rand.nextDouble() * 2;
			double targetY = rand.nextDouble() * 2;
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
					bankEdge = net.addEdge(this, toAdd);
//					System.out.println("I am " + this + " and I just joined " + toAdd);
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
		GridPoint pointWithMostCBanks = pt;
		
		if (cBank == null){
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
	@ScheduledMethod(start = 1, interval = 12)
	public void consumer_move_1() throws Exception{
		consumerMove();
		shocked = false;
		panicPanic = false;
		liquidityPanic = false;
		panicFlag = false;
		purePanic = false;
		shockedButStanding = false;
		placeInLine = 0;
		estimatedPanicWithdrawal = 0.0001;
		expectedWithdrawalsP = 0.0001;
		assetsFutureP = 0.0001;
		expectedWithdrawalsL = 0.0001;
		assetsFutureL = 0.0001;
		trueLiquidityDemand = 0.0001;
		consumerPBCount = 0;
		neighborPanicCount = 0;
		neighborShockCount = 0;
		othersConsumption = 0.0001;
		neighborPanicProportion = 0.0000001;
	}
	
	@ScheduledMethod(start = 4, interval = 12)
	public void consumer_initialNet_4() throws Exception{
		discoverInitialNet();
		calculateBankAssets();
	}
	
	@ScheduledMethod(start = 7, interval = 12)
	public void consumer_pblm_7() throws Exception{
		panicBasedConsumption();
	}
	
	/** This is the first basic scheduled method to be called.
	 * The consumer initializes its consumption demand.
	 * The consumer than moves around to search for banks or just move
	 * This provides the basis of consumers passing information to each other. 
	 */
	@ScheduledMethod(start = 8, interval = 12)
	public void consumer_payBills_8() throws Exception{
		payingBills();
	}
	
	//one of last scheduled methods
	/** This method forces a consumer to leave the environment if he or she is bankrupt.
	 * If a consumer is bankrupt, their account is removed from their cBank.
	 * If consumers reach a situation where isBankrupt is set to true, the contents of their bank accounts should have been withdrawn already.
	 * @throws Exception 
	 * 
	 */
	@ScheduledMethod(start = 11, interval = 12)
	public void consumer_check_11() throws Exception{
		if (isBankrupt){
//			System.out.println("I am " + this + " and I just went bankrupt so I am about to leave my bank " + getBank());
			leaveBank(getBank());
			DD.removeConsumer();
			consumerDie();
		}
		else{
			age++;
			transferMoney();
		}
	}
	
	

}
