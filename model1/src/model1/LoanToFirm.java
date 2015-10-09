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
	private boolean paid;
	private String loanId;
	
	public LoanToFirm(Firm firm, double remainingBalance, double payment, String loanId){
		this.firm = firm;
		this.remainingBalance = remainingBalance;
		this.payment = payment;
		this.loanId = loanId;
		
		//Do I want to require payment at initialization?
		paid = false;
	}
	
	public void makePaid(){
		paid = true;
	}
	
	public boolean isPaid(){
		return paid;
	}
	
	public Firm getFirm(){
		return firm;
	}
	
	public String getId(){
		return loanId;
	}
	
	public double getPayment(){
		return payment;
	}
	
	public double getRemainingBalance(){
		return remainingBalance;
	}
	
	public double receivePayment(double amount){
		if (amount == payment){
			makePaid();
		}
		else{
			;
		}
		remainingBalance -= amount;
		if (remainingBalance <= 0){
			makePaid();
			return -1.0;
			//destroy this loan since it is paid
		}
		if (isPaid()){
			return payment;
		}
		return amount;
	}
}