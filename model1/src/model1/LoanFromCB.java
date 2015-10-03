/**
 * 
 */
package model1;

/**
 * @author stevenyee
 *
 */
public class LoanFromCB {
	private CommercialBank bank;
	private double remainingBalance;
	private double payment;
	private boolean isPaid;
	
	public LoanFromCB(CommercialBank bank, double remainingBalance, double payment){
		this.bank = bank;
		this.remainingBalance = remainingBalance;
		this.payment = payment;
		
		//Do I want to require payment at initialization?
		isPaid = false;
	}
	
	

}
