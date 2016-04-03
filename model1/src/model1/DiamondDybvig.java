/**
 * 
 */
package model1;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;
import repast.simphony.util.ContextUtils;

/**
 * @author stevenyee
 *
 */
public class DiamondDybvig {

	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	private int placeInLine = 0;
	private double initialWithdrawals = 0.0;
	private double totalWithdrawals = 0.0;
	
	private double consumerInitialEndowment;
	
	private double bankShortTermAssets = 0.0;
	private double bankLongTermAssets = 0.0;

	private double consumerLongTermPayout; // needs to be lower than bank value
	private double bankShortTermPayout; //needs to be lower than consumer value
	private double bankLongTermPayout; // needs to be higher than consumer value
	
	private double bankCost2;
	
	private double panicEstimate = 0.0;
	private double averagePanicEstimate = 0.0;
	private int panicEstimateCount = 0;
	
	private double probWithdrawal;
	
	private int consumerCount = 0;
	private boolean isUnnecessary;
	private boolean bankFail = false;
	
	
	
	public DiamondDybvig(ContinuousSpace<Object> space, Grid<Object> grid, double consumerLongTermPayout, double consumerInitialEndowment, double bankShortTermPayout, double bankLongTermPayout, double bankCost2, double consumerInitialCount, double probWithdrawal, int originalConsumers, double blank){
		this.space = space;
		this.grid = grid;
		this.consumerLongTermPayout = consumerLongTermPayout;
		this.bankShortTermPayout = bankShortTermPayout;
		this.bankLongTermPayout = bankLongTermPayout;
		this.bankCost2 = bankCost2 * originalConsumers;
		this.probWithdrawal = probWithdrawal;
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
	
	public int getPanicEstimatesCount(){
		return panicEstimateCount;
	}
	
	public void addPanicEstimateCount(){
		panicEstimateCount +=1;
	}
	
	public void resetPanicEstimatesCount(){
		panicEstimateCount = 0;
	}
	
	public double getPanicEstimate(){
		return panicEstimate;
	}
	
	public void addPanicsEstimate(double amount){
		panicEstimate += amount;
	}
	
	public void resetPanicsEstimate(){
		panicEstimate = 0;
	}
	
	public double getAveragePanicEstimate(){
		if (panicEstimateCount == 0){
			averagePanicEstimate =  0.0;
			return averagePanicEstimate;
		}
		averagePanicEstimate = panicEstimate / panicEstimateCount;
		return averagePanicEstimate;
	}
	
	
	public void addInitialWithdrawals(double amount){
		initialWithdrawals -= amount;
	}
	
	public void resetInitialWithdrawals(){
		initialWithdrawals = 0.0;
	}
	//consumer can see total withdrawals
	public double getInitialWithdrawals(){
		return initialWithdrawals;
	}
	
	
	public void addTotalWithdrawals(double amount){
		totalWithdrawals -= amount;
	}
	
	public void resetTotalWithdrawals(){
		totalWithdrawals = 0.0;
	}
	//consumer can see total withdrawals
	public double getTotalWithdrawals(){
		return totalWithdrawals;
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
		
		double assetsFuture = bankShortTermPayout * tempBankShort + bankLongTermPayout * tempBankLong - bankCost2;
		double expectedWithdrawals = probWithdrawal * consumerCount;
		
		isUnnecessary = (assetsFuture >= expectedWithdrawals);
	}
	
	public boolean getUnnecessary(){
		return isUnnecessary;
	}
	
	public boolean getBankFail(){
		return bankFail;
	}
	
	public void updateBankFail(){
		bankFail = true;
	}
	
	@ScheduledMethod(start = 2, interval = 12)
	public void DD_reset_2() throws Exception{
		initialWithdrawals = 0.0;
		placeInLine = 0;
		panicEstimate = 0.0;
		averagePanicEstimate = 0.0;
		panicEstimateCount = 0;
		bankFail = false;
		totalWithdrawals = 0.0;
	}
	
	@ScheduledMethod(start = 6, interval = 12)
	public void DD_checkUnnecessary_6() throws Exception{
		checkUnnecessary();
	}
	
	@ScheduledMethod(start = 12, interval = 12)
	public void DD_deathCheck() throws Exception{
		checkUnnecessary();
		Context<Object> context = ContextUtils.getContext(this);
		if ((context.getObjects(Consumer.class).size() == 0) || (context.getObjects(CommercialBank.class).size() == 0)){
			RunEnvironment.getInstance().endRun();
		}
		RunEnvironment.getInstance().endAt(150);
	}
	
}
