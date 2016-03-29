/**
 * 
 */
package model1;

import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

/**
 * @author stevenyee
 *
 */
public class DiamondDybvig {

	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	private int consumerCount = 0;
	
	private int placeInLine = 0;
	private double initialWithdrawals = 0.0;
	
	private double consumerInitialEndowment;
	
	private double bankShortTermAssets = 0.0;
	private double bankLongTermAssets = 0.0;

	private double consumerLongTermPayout; // needs to be lower than bank value
	private double bankShortTermPayout; //needs to be lower than consumer value
	private double bankLongTermPayout; // needs to be higher than consumer value
	
	private double bankCost1;
	private double bankCost2;
	
	private boolean isUnnecessary;
	
	
	
	public DiamondDybvig(ContinuousSpace<Object> space, Grid<Object> grid, double consumerLongTermPayout, double consumerInitialEndowment, double bankShortTermPayout, double bankLongTermPayout, double bankCost1, double bankCost2, double consumerInitialCount){
		this.space = space;
		this.grid = grid;
		this.consumerLongTermPayout = consumerLongTermPayout;
		this.bankShortTermPayout = bankShortTermPayout;
		this.bankLongTermPayout = bankLongTermPayout;
		
		this.bankCost1 = bankCost1;
		this.bankCost2 = bankCost2;
		
		this.consumerInitialEndowment = consumerInitialEndowment;
	}
	
	
	public int getConsumerCount(){
		return consumerCount;
	}
	
	public void addConsumer(){
		consumerCount++;
	}
	
	public void removeConsumer(){
		consumerCount--;
	}
	
	public void resetPlaceInLine(){
		placeInLine = 0;
	}
	
	public int getPlaceInLine(){
		return placeInLine;
	}
	
	public int addPlaceInLine(){
		placeInLine++;
		return placeInLine;
	}
	
	
	public void addInitialWithdrawals(double amount){
		initialWithdrawals += amount;
	}
	
	public void resetInitialWithdrawals(){
		initialWithdrawals = 0.0;
	}
	//consumer can see total withdrawals
	public double getInitialWithdrawals(){
		return initialWithdrawals;
	}
		
	
	
	public void addBankShort(double amount){
		bankShortTermAssets += amount;
	}
	
	public void setBankShort(double amount){
		bankShortTermAssets = amount;
	}
	
	public void removeBankShort(double amount){
		bankShortTermAssets -= amount;
	}
	
	public double getBankShort(){
		return bankShortTermAssets;
	}
	
	
	public void addBankLong(double amount){
		bankLongTermAssets += amount;
	}
	
	public void setBankLong(double amount){
		bankLongTermAssets = amount;
	}
	
	public void removeBankLong(double amount){
		bankLongTermAssets -= amount;
	}
	
	public double getBankLong(){
		return bankLongTermAssets;
	}
	
	
	public void checkUnnecessary(){
		double tempWithdrawals = -initialWithdrawals;
		double tempBankShort = bankShortTermAssets;
		double tempBankLong = bankLongTermAssets;
		
		if (tempWithdrawals > tempBankShort){
			tempWithdrawals -= tempBankShort;
			tempBankShort = 0.0;
			if (tempWithdrawals <= tempBankLong){
				tempBankLong -= tempWithdrawals;
				tempWithdrawals = 0.0;
			}
			else{
				tempWithdrawals -= tempBankLong;
				tempBankLong = 0.0;
			}
		}else{
			tempBankShort -= tempWithdrawals;
			tempWithdrawals = 0.0;
		}
		
		double assetsFuture = bankShortTermPayout * (tempBankShort - bankCost1) + bankLongTermPayout * tempBankLong - bankCost2;
		double expectedWithdrawals = (consumerLongTermPayout - 1) * consumerInitialEndowment * consumerCount;
		
		isUnnecessary = (assetsFuture >= expectedWithdrawals);
	}
	
	public boolean getUnnecessary(){
		return isUnnecessary;
	}
	
	@ScheduledMethod(start = 2, interval = 10)
	public void DD_reset_2() throws Exception{
		initialWithdrawals = 0.0;
		placeInLine = 0;
	}
	
	@ScheduledMethod(start = 6, interval = 10)
	public void DD_checkUnnecessary_6() throws Exception{
		checkUnnecessary();
	}
	
}
