/**
 * 
 */
package model1;

/**
 * @author stevenyee
 *
 */
public class LoanToFirm {
	private Firm firm;
	private double remainingBalance;
	private double payment;
	private boolean isPaid;
	
	public LoanToFirm(Firm firm, double remainingBalance, double payment){
		this.firm = firm;
		this.remainingBalance = remainingBalance;
		this.payment = payment;
		
		//Do I want to require payment at initialization?
		isPaid = false;
	}
	

}
