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
	
	private DiamondDybvig DD;
	private double bankCost2;
	
	private int consumerCount = 0;
	
	private HashSet<Consumer> Consumers;
	
	
	/** This method instantiates a Commercial Bank ("cBank").
	 * @param space
	 * @param grid
	 * @param reserves
	 * @param annualSavingsYield
	 * @param loanRate
	 * @param loanYears
	 * @throws Exception 
	 */
	public CommercialBank(ContinuousSpace<Object> space, Grid<Object> grid, double reserves, double consumerShortTermPayout, double consumerLongTermPayout, double bankShortTermPayout, double bankLongTermPayout, double bankCost2, DiamondDybvig DD, int consumerCount) throws Exception{
		this.space = space;
		this.grid = grid;
		
		this.consumerShortTermPayout = consumerShortTermPayout;
		this.consumerLongTermPayout = consumerLongTermPayout;
		this.bankShortTermPayout = bankShortTermPayout;
		this.bankLongTermPayout = bankLongTermPayout;
		
		
		this.DD = DD;
		
		this.consumerCount = consumerCount;
		this.bankCost2 = bankCost2 * consumerCount;
		shortTermAssets  = reserves;
		Consumers = new HashSet<Consumer>();
		
	}
	
	/** This method creates an account at this cBank for a consumer.
	 * If the consumer already has an account, nothing happens.
	 * @param holder Consumer that wishes to create an account
	 * @param amount Initial deposit is equal to Consumer's cash pile.
	 * @return return true if account created, returns false if consumer already has an account at this bank
	 * @throws Exception Throws exception if negative amount is given.
	 */
	public boolean addAccount(Consumer holder, double shortTerm, double longTerm) throws Exception{
//		System.out.println("cBank trying to add " + holder + " with " + amount);
		if (!(Consumers.contains(holder))){
			Consumers.add(holder);
			shortTermAssets += shortTerm;
			shortTermLiabilities += shortTerm;
			longTermAssets += longTerm;
			longTermLiabilities += longTerm;
			
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
		if (amount >= -1.0){
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
	
	public double getTotalAssetsAfterInitialDraws(){
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
	
	public void payOperatingCosts(double amount){
		if (amount > shortTermAssets){
			shortTermAssets = 0;
			getTotalAssets();
			if (amount > longTermAssets* bankShortTermPayout){
				longTermAssets = -1;
				getTotalAssets();		
			}
			else{
				longTermAssets -= amount / bankShortTermPayout;
				getTotalAssets();
			}
		}
		else{
			shortTermAssets -= amount;
			getTotalAssets();
		}
	}
	
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
//		System.out.println(this + " My leftover is " + leftOver);
		if (leftOver >= -1.0){
			leftOver -= removeShortTerm(leftOver);
			if (leftOver >= 0.0){
//				System.out.println(this + " had to tap into its long term assets to cover the remaining " + leftOver);
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
	
	
	public void transfers(){
		double tempBankShortTerm = shortTermAssets;
		
		//ADD IN BANK PENALTY HERE THIS IS THE LITTLE D
		
		
		shortTermAssets = longTermAssets * bankLongTermPayout; //ADD IN THE SECOND STATIONARITY PENALTY HERE
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
//		System.out.println("BANK BLOW UP " + this);
		DD.updateBankFail();
		Iterator<Consumer> consumerList = Consumers.iterator();
		if (consumerList != null){
			while (consumerList.hasNext()){
				Consumer consumer = consumerList.next();
				consumer.forcedToLeaveBank(this);
				consumerList.remove();
			}
		}
	}
	
	@ScheduledMethod(start = 3, interval = 12)
	public void bank_payCost2_3() throws Exception{
		if (age > 0){
			payOperatingCosts(bankCost2);
		}
	}
	
	@ScheduledMethod(start = 5, interval = 12)
	public void bank_updatesDD_5() throws Exception{
		DD.setBankShort(shortTermAssets);
		DD.setBankLong(longTermAssets);
		getTotalAssetsAfterInitialDraws();
	}

	@ScheduledMethod(start = 9, interval = 12)
	public void bank_End_9() throws Exception{
		if (longTermAssets <= -1){
			DD.updateBankFail();
			removeAllConsumers();
		}
	}
	
	/** This scheduled status method determines if a cBank has gone bankrupt or not.
	 * If it has gone bankrupt, it has already called collectFullLoans() so all loans have been destroyed.
	 * It now must remove all Consumer accounts.
	 * If the cBank is not bankrupt, it pays interest on all Consumer accounts.
	 * @throws Exception
	 */
	@ScheduledMethod(start = 10, interval = 12)
	public void bank_End_10() throws Exception{
		if (longTermAssets <= -1){
			cBankDie();
		}
		else{
			age++;
			transfers();
		}
	}


}
