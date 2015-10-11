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
	private String loanId;
	
	public LoanFromCB(CommercialBank cBank, double remainingBalance, double payment, String loanId){
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
	
	public void makePaid(){
		paid = true;
	}
	
	public boolean isPaid(){
		return paid;
	}
	
	public CommercialBank getBank(){
		return cBank;
	}
	
	public double getRemainingBalance(){
		return remainingBalance;
	}
	
	public String getId(){
		return loanId;
	}
	
	public double makePayment(double amount) throws Exception{
		if (amount == payment){
			makePaid();
			cBank.receivePayment(loanId, payment);
		}
		else{
			cBank.receivePayment(loanId, amount);
		}
		remainingBalance -= amount;
		if (remainingBalance <= 0.0){
			makePaid();
			return -1.0;
		}
		if (isPaid()){
			return payment;
		}
		return amount;
	}
	
	

}
