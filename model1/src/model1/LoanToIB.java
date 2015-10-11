/**
 * 
 */
package model1;

/**
 * @author stevenyee
 *
 */
public class LoanToIB {
	private InvestmentBank bank;
	private double remainingBalance;
	private double payment;
	private boolean paid;
	private String loanId;
	
	public LoanToIB(InvestmentBank bank, double remainingBalance, double payment, String loanId){
		this.bank = bank;
		this.remainingBalance = remainingBalance;
		this.payment = payment;
		this.loanId = loanId;
		
		//Do I want to require payment at initialization?
		paid = false;
	}
	
	public boolean isPaid(){
		return paid;
	}
	
	public void makePaid(){
		paid = true;
	}
	
	public String getId(){
		return loanId;
	}
	
	public InvestmentBank getBank(){
		return bank;
	}
	
	public double getPayment(){
		return payment;
	}
	
	public double getRemainingBalance(){
		return remainingBalance;
	}
	
	//this assumes that all iBanks attempt to make payments on loans.
	public double receivePayment(double amount){
		paid = false;
		if (amount == payment){
			makePaid();
		}
		else{
			//paid = false;
		}
		remainingBalance -= amount;
		if (remainingBalance <= 0){
			makePaid();
			return -1.0;
			//destroy this loan since it is paid
		}
		if (paid){
			return payment;
		}
		return amount;
	}
	
}
