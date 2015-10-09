/**
 * 
 */
package model1;

import java.util.Random;

import org.apache.commons.math3.distribution.NormalDistribution;

import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

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
		
		this.salaryCurve = new NormalDistribution(salary, CONSUMER_DEVIATION_PERCENT*salary);
		this.salary = salaryCurve.sample();
		this.expenseCurve = new NormalDistribution(salary - averageSavings, CONSUMER_DEVIATION_PERCENT*(salary - averageSavings));
		this.expenses = expenseCurve.sample();
	}
	
	public void calculateSalary(){
		salary = salaryCurve.sample();
	}
	
	public void calculateExpenses(){
		expenses = expenseCurve.sample();
	}
	
	public double calculateDisaster(){
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
	
	public double calculateNet(){
		double net = salary - expenses - calculateDisaster();
		return net;
	}
	
	public void depositSavings(double amount){
		if (cBank != null){
			cBank.deposit(this, amount);
		}
		else{
			//This is the case where the consumer has no bank and stores his savings under his mattress
			addCash(amount);
		}
	}
	
	
	//if consumer was looking at paying this back toward a loan, I would want to make sure cBank or whomever received their money before the consumer was deleted
	//somewhere else, there will need to be a check to make sure creditor received the full amount they expected
	public void withdrawSavings(double amount){
		if (cBank != null){
			double withdrawal = cBank.withdraw(this, amount);
			if (withdrawal != amount){
				//add a method from creditor and pass it actualAmount so they get their money back before the consumer is destroyed
				//for example cBank.addAccount(savings(actualAmount), actualAMount)
				isBankrupt = true;
			}
			//need to handle passing amount to creditor
		}
		else{
			if (cash >= amount){
				double actualAmount = removeCash(amount);
				//need to handle passing amount to creditor
			}
			else{
				double actualAmount = removeCash(cash);
				//remove consumer//add a method from creditor and pass it actualAmount so they get their money back before the consumer is destroyed
				//for example cBank.addAccount(savings(actualAmount), actualAMount)
				isBankrupt = true;
			}
		}
	}
	
	//Before this method is called, you must check to make sure the consumer does not already have a banking relationship with cBank
	public boolean joinBank(CommercialBank cBankNew){
		//I may want to eventually switch this to searching a list of the consumer's cBanks. This assumes each consumer has only one cBank
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
	
	public boolean leaveBank(CommercialBank cBankDead){
		//I may want to eventually switch this to searching a list of the consumer's cBanks. This assumes each consumer has only one cBank
		if (this.cBank == cBankDead){
			cBank = null;
			return true;
		}
		else{
			return false;
		}
	}
	
	public CommercialBank getBank(){
		return cBank;
	}
	
	public void addCash(double amount){
		cash += amount;
	}
	
	public double removeCash(double amount){
		//error checking must be done before call
		cash -= amount;
		return amount;
	}
	
	//first scheduled method
	public void consumer_tick_9(){
		calculateSalary();
		calculateExpenses();
		double net = calculateNet();
		if (net < 0){
			withdrawSavings(net);
		}
		else{
			depositSavings(net);
		}
		if (getBank() == null){
			//search for a bank
			//joinBank()
		}
	}
	
	//one of last scheduled methods
	public void consumer_check_104(){
		if (isBankrupt){
			getBank().removeAccount(this);
			//consumer.die()			
		}
	}
	
	

}
