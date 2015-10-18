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
		
		NetworkBuilder<Object> cBanks_iBanks_network = new NetworkBuilder<Object>("cBanks_iBanks network", context, true);
		cBanks_iBanks_network.buildNetwork();
		
		NetworkBuilder<Object> iBanks_firms_network = new NetworkBuilder<Object>("iBanks_firms network", context, true);
		iBanks_firms_network.buildNetwork();
		
		
		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);
		ContinuousSpace<Object> space = spaceFactory.createContinuousSpace("space", context, new RandomCartesianAdder<Object>(), new repast.simphony.space.continuous.WrapAroundBorders(), 50, 50);
		
		GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);
		//correct import: import repast.simphony.space.grid.WrapAroundBorders;
		Grid<Object> grid = gridFactory.createGrid("grid", context, new GridBuilderParameters<Object>(new WrapAroundBorders(), new SimpleGridAdder<Object>(), true, 50, 50));
		
		int consumerCount = 1;
		for (int i = 0; i < consumerCount; i++){
			context.add(new Consumer(space, grid, 10000.0, 1000.0, 0.05, 1000.0, 0.0, 1.4, 0.0, 0.10));
		}
		
		
		int cBankCount = 1;
		for (int i = 0; i < cBankCount; i++){
			try {
				context.add(new CommercialBank(space, grid, 20000.0, 0.01, 0.03, 5.0));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		int iBankCount = 1;
		for (int i = 0; i < iBankCount; i++){
			try {
				context.add(new InvestmentBank(space, grid, 100.0, 0.05, 0.03, 5.0, 5.0));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		int firmCount = 1;
		for (int i = 0; i < firmCount; i++){
			context.add(new Firm(space, grid, 10000.0, 10000.0, 0.05, 3000.0, 0.0, 1.4, 0.0, 0.70, 0.05, 5.0, 0.00));
		}
		
		for (Object obj : context){
			NdPoint pt = space.getLocation(obj);
			grid.moveTo(obj,  (int)pt.getX(), (int)pt.getY());
		}
		
		
		return context;
	}

}
