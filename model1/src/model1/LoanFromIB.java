/**
 * 
 */
package model1;

/**
 * @author stevenyee
 *
 */
public class LoanFromIB {
	private InvestmentBank iBank;
	private double remainingBalance;
	private double payment;
	private boolean paid;
	private int loanId;
	
	public LoanFromIB(InvestmentBank iBank, double remainingBalance, double payment, int loanId){
		this.iBank = iBank;
		this.remainingBalance = remainingBalance;
		this.payment = payment;
		this.loanId = loanId;
		
		//Do I want to require payment at initialization?
		paid = false;
	}
	
	public int makePayment(double amount){
		if (amount == payment){
			paid = true;
			iBank.receiveFullPayment(loanId);
		}
		else{
			paid = false;
			iBank.receivePartialPayment(loanId, amount);
		}
		remainingBalance -= payment;
		if (remainingBalance <= 0){
			return 1;
		}
		if (paid){
			return 0;
		}
		return -1;
	}
	
}
