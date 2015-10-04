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
	private String loanId;
	
	public LoanFromIB(InvestmentBank iBank, double remainingBalance, double payment, String loanId){
		this.iBank = iBank;
		this.remainingBalance = remainingBalance;
		this.payment = payment;
		this.loanId = loanId;
		
		//Do I want to require payment at initialization?
		paid = false;
	}
	
	public double makePayment(double amount) throws Exception{
		if (amount == payment){
			paid = true;
			iBank.receivePayment(loanId, payment);
		}
		else{
			paid = false;
			iBank.receivePayment(loanId, amount);
		}
		remainingBalance -= amount;
		if (remainingBalance <= 0){
			return -1.0;
		}
		if (paid){
			return payment;
		}
		return amount;
	}
	
	public double getRemainingBalance(){
		return remainingBalance;
	}
	
	public InvestmentBank getInvestmentBank(){
		return iBank;
	}
	
	public double getPayment(){
		return payment;
	}
	
	public boolean isPaid(){
		return paid;
	}
	
	public String getId(){
		return loanId;
	}
	
	
	
}
