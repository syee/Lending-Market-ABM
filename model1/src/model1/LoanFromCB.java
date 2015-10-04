/**
 * 
 */
package model1;

/**
 * @author stevenyee
 *
 */
public class LoanFromCB {
	private CommercialBank cBank;
	private double remainingBalance;
	private double payment;
	private boolean paid;
	private int loanId;
	
	public LoanFromCB(CommercialBank cBank, double remainingBalance, double payment, int loanId){
		this.cBank = cBank;
		this.remainingBalance = remainingBalance;
		this.payment = payment;
		this.loanId = loanId;
		
		//Do I want to require payment at initialization?
		paid = false;
	}
	
	public double getPayment(){
		return payment;
	}
	
	public boolean isPaid(){
		return paid;
	}
	
	public int makePayment(double amount){
		if (amount == payment){
			paid = true;
			cBank.receiveFullPayment(loanId);
		}
		else{
			paid = false;
			cBank.receivePartialPayment(loanId, amount);
		}
		remainingBalance -= amount;
		if (remainingBalance <= 0){
			return 1;
		}
		if (paid){
			return 0;
		}
		return -1;
	}
	
	

}
