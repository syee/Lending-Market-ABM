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
			addCash(amount);
		}
	}
	
	public void withdrawSavings(double amount){
		if (cBank != null){
			double withdrawal = cBank.withdraw(this, amount);
			if (withdrawal != amount){
				//remove consumer
			}
		}
		else{
			if (cash >= amount){
				removeCash(amount);
			}
			else{
				//remove consumer
			}
		}
	}
	
	public void joinBank(CommercialBank cBank){
		if(cBank.addAccount(this, cash)){
			removeCash(cash);
		}
		else{
			//already joined bank
		}
	}
	
	public void addCash(double amount){
		cash += amount;
	}
	
	public void removeCash(double amount){
		//error checking must be done before call
		cash -= amount;
	}
	
	

}
