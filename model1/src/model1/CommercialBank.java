/**
 * 
 */
package model1;

import java.util.HashMap;
import java.util.LinkedList;

import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

/**
 * @author stevenyee
 *
 */
public class CommercialBank {
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	private double reserves;
	private HashMap<Consumer, Double> Consumers;
	private double savingsYield;
	private double loanRate;
	private LinkedList<LoanToIB> loansToIB;
	
	
	
	
	public CommercialBank(ContinuousSpace<Object> space, Grid<Object> grid, double reserves, double savingsYield, double loanRate){
		this.space = space;
		this.grid = grid;
		this.reserves = reserves;
		this.savingsYield = savingsYield;
		this.loanRate = loanRate;
		Consumers = new HashMap<Consumer, Double>();
		loansToIB = new LinkedList<LoanToIB>();
	}

}
