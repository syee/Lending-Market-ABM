/**
 * 
 */
package model1;

import java.util.LinkedList;

import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

/**
 * @author stevenyee
 *
 */
public class InvestmentBank {
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	private double reserves;
	private double loanRate;
	private LinkedList<LoanFromCB> loansFromCB;
	private LinkedList<LoanToFirm> loansToFirms;
	
	public InvestmentBank(ContinuousSpace<Object> space, Grid<Object> grid){
		this.space = space;
		this.grid = grid;
		loansFromCB = new LinkedList<LoanFromCB>();
		loansToFirms = new LinkedList<LoanToFirm>();
	}

}
