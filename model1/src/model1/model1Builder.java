package model1;

import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.SimpleGridAdder;
import repast.simphony.space.grid.WrapAroundBorders;

public class model1Builder implements ContextBuilder<Object> {
	
//	@Override
//	public Context build(Context<Object> context) {
//		return context;
//	}

	@Override
	public Context build(Context<Object> context) {
		context.setId("model1");
		
		
		NetworkBuilder<Object> consumers_cBanks_network = new NetworkBuilder<Object>("consumers_cBanks network", context, true);
		consumers_cBanks_network.buildNetwork();
		
		
		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);
		ContinuousSpace<Object> space = spaceFactory.createContinuousSpace("space", context, new RandomCartesianAdder<Object>(), new repast.simphony.space.continuous.WrapAroundBorders(), 50, 50);
		
		GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);
		//correct import: import repast.simphony.space.grid.WrapAroundBorders;
		Grid<Object> grid = gridFactory.createGrid("grid", context, new GridBuilderParameters<Object>(new WrapAroundBorders(), new SimpleGridAdder<Object>(), true, 50, 50));
		
		Parameters params = RunEnvironment.getInstance().getParameters();
		
		double shortTermEndowment = (double)params.getValue("Consumer Initial Endowment");
		double consumerSalary = (double)params.getValue("Consumer Income");
		double consumerDeviationPercent = (double)params.getValue("Consumer Distribution Deviation");
		double consumerConsumptionMean = (double)params.getValue("Consumer Mean Rate of Consumption");
		double consumerShockMultiplier = (double)params.getValue("Consumer Shock Multiplier");
		double consumerShockProbability = (double)params.getValue("Consumer Shock Probability");
		double consumerShortTermRate = (double)params.getValue("Consumer Short Term Payout");
		double consumerLongTermRate = (double)params.getValue("Consumer Long Term Payout");
		int consumerGroupSize = (Integer)params.getValue("Consumer Group Size");
		int consumerCount = (Integer)params.getValue("Consumer Count");
		
		int bankCount = 1;
		double bankReserves = (double)params.getValue("Bank Initial Endowment");
		double bankShortTermRate = (double)params.getValue("Bank Short Term Asset Return");
		double bankLongTermRate = (double)params.getValue("Bank Long Term Asset Return");
		
		double bankCost2 = (double)params.getValue("Bank Cost 2");
		double blank = (double)params.getValue("Blank");
		
		double probWithdrawal = (double)params.getValue("Expected Average Withdrawal");
		
		boolean allConsumersVisible = (boolean)params.getValue("All Consumers Visible");
		boolean bankVisible = (boolean)params.getValue("Bank Assets Visible");
		
		DiamondDybvig DD = new DiamondDybvig(space, grid, consumerLongTermRate, shortTermEndowment, bankShortTermRate, bankLongTermRate, bankCost2, consumerCount, probWithdrawal, blank);

		context.add(DD);
		
		
		for (int i = 0; i < consumerCount; i++){
			context.add(new Consumer(space, grid, consumerSalary, shortTermEndowment, consumerDeviationPercent, consumerConsumptionMean, consumerShockMultiplier, consumerShockProbability, consumerShortTermRate, consumerLongTermRate, consumerGroupSize, DD, bankShortTermRate, bankLongTermRate, bankCost2, probWithdrawal, allConsumersVisible, bankVisible));
			DD.addConsumer();
		}
		
		
		for (int i = 0; i < bankCount; i++){
			try {
				context.add(new CommercialBank(space, grid, bankReserves, consumerShortTermRate, consumerLongTermRate, bankShortTermRate, bankLongTermRate, bankCost2, DD, consumerCount));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		for (Object obj : context){
			NdPoint pt = space.getLocation(obj);
			grid.moveTo(obj,  (int)pt.getX(), (int)pt.getY());
		}
		
		
		return context;
	}

}
