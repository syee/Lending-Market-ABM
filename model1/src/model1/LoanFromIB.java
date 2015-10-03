/**
 * 
 */
package model1;

/**
 * @author stevenyee
 *
 */
public class LoanFromIB {
	private InvestmentBank bank;
	private double remainingBalance;
	private double payment;
	private boolean isPaid;
	
	public LoanFromIB(InvestmentBank bank, double remainingBalance, double payment){
		this.bank = bank;
		this.remainingBalance = remainingBalance;
		this.payment = payment;
		
		//Do I want to require payment at initialization?
		isPaid = false;
	}
	
}
