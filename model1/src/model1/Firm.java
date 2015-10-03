/**
 * 
 */
package model1;

import java.util.LinkedList;

import org.apache.commons.math3.distribution.NormalDistribution;

import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

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
	private NormalDistribution revenueCurve;
	private NormalDistribution expenseCurve;
	private double FIRM_DEVIATION_PERCENT;
	private double averageProfits;
	private LinkedList<LoanFromIB> loansFromIB;
	
	public Firm(ContinuousSpace<Object> space, Grid<Object> grid, double revenue, double expenses, double reserves, double FIRM_DEVIATION_PERCENT, double averageProfits){
		this.space = space;
		this.grid = grid;
		this.revenue = revenue;
		this.expenses = expenses;
		this.reserves = reserves;
		this.FIRM_DEVIATION_PERCENT = FIRM_DEVIATION_PERCENT;
		this.averageProfits = averageProfits;
		
		this.revenueCurve = new NormalDistribution(revenue, FIRM_DEVIATION_PERCENT*revenue);
		this.revenue = revenueCurve.sample();
		this.expenseCurve = new NormalDistribution(revenue - averageProfits, FIRM_DEVIATION_PERCENT*(revenue - averageProfits));
		this.expenses = expenseCurve.sample();
		
		loansFromIB = new LinkedList<LoanFromIB>();
	}

}
