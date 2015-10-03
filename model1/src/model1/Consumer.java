/**
 * 
 */
package model1;

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
	private CommercialBank bank;
	
	private NormalDistribution salaryCurve;
	private NormalDistribution expenseCurve;
	private double CONSUMER_DEVIATION_PERCENT;
	private double averageSavings;
	
	
	public Consumer(ContinuousSpace<Object> space, Grid<Object> grid, double salary, double cash, double CONSUMER_DEVIATION_PERCENT, double averageSavings){
		this.space = space;
		this.grid = grid;
		this.cash = cash;
		this.CONSUMER_DEVIATION_PERCENT = CONSUMER_DEVIATION_PERCENT;
		this.averageSavings = averageSavings;
		
		this.salaryCurve = new NormalDistribution(salary, CONSUMER_DEVIATION_PERCENT*salary);
		this.salary = salaryCurve.sample();
		this.expenseCurve = new NormalDistribution(salary - averageSavings, CONSUMER_DEVIATION_PERCENT*(salary - averageSavings));
		this.expenses = expenseCurve.sample();
	}

}
