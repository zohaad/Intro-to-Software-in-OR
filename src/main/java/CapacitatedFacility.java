/*
    Zohaad Fazal
    ID: i6107208
    Date: January 16, 2020
    Exercise B0: Capacitated Facility 1
    This code is my own work, it was developed without using or copying
    code from other students or other resources.
*/

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;

public class CapacitatedFacility {

    public static void solveCapacitatedFacility() throws IloException {
        IloCplex cplex = new IloCplex();
        cplex.setOut(null); // sets output to null

        int numPlants = 2;
        int numWarehouses = 2;
        int numMarkets = 6;

        // store names to print out results later
        String[] plants = {"Denver", "Wichita"};
        String[] warehouses = {"Chicago", "Salt Lake City"};
        String[] markets = {"Seattle", "Sacramento", "Houston", "Toronto", "Miami", "Detroit"};

        // define parameters
        // demand for the market
        int[] demandMarket = {480, 420, 220, 500, 450, 320}; // Seattle, Sacramento, Houston, Toronto, Miami, Detroit

        // cost for 1000 units to transport from the plants to the warehouses
        int[][] costPlantsToWarehouses = {
                {105, 68}, // plant Denver to warehouses Chicago and Salt Lake City
                {87, 75}, // plant Wichita to warehouses Chicago and Salt Lake City
        };
        // cost for 1000 units to transport from the warehouses to the markets
        int[][] costWarehousesToMarkets = {
                {165, 183, 124, 86, 132, 67}, // warehouse Chicago to markets (order listed in line 16)
                {110, 75, 132, 210, 153, 195}, // warehouse Salt Lake City to markets (order listed in line 16)
        };

        // define variables
        // atomic flow from plants to warehouses
        IloNumVar[][] flowPlantsToWarehouses = new IloNumVar[numPlants][];
        for (int i = 0; i < numPlants; i++) {
            // largest capacity is 2500, that of Denver with high capacity.
            // note: we could have made it higher (constraints take care of the rest) but this is a safe upper bound
            flowPlantsToWarehouses[i] = cplex.numVarArray(numWarehouses, 0, 2500, IloNumVarType.Int);
        }
        // atomic flow from warehouses to markets
        IloNumVar[][] flowWarehousesToMarkets = new IloNumVar[numWarehouses][];
        for (int i = 0; i < numWarehouses; i++) {
            // largest capacity is 2000, when either warehouse is expanded to have a higher capacity
            // note: we could have made it higher (constraints take care of the rest) but this is a safe upper bound
            flowWarehousesToMarkets[i] = cplex.numVarArray(numMarkets, 0, 2000, IloNumVarType.Int);
        }
        // decision variable whether to expand the Denver plant
        IloNumVar decisionIncreaseDenverPlantCapacity = cplex.numVar(0, 1, IloNumVarType.Bool);
        // decision variable whether to open a new plant in Wichita
        IloNumVar decisionOpenWichitaPlant = cplex.numVar(0, 1, IloNumVarType.Bool);
        // decision variables whether to increase the warehouse capacities in Chicago and Salt Lake City
        IloNumVar[] decisionIncreaseWarehouseCapacities = cplex.numVarArray(numWarehouses, 0, 1, IloNumVarType.Bool);

        // add objective function
        // costs from plants to warehouses
        IloLinearNumExpr expr = cplex.linearNumExpr();
        for (int i = 0; i < numPlants; i++) {
            for (int j = 0; j < numWarehouses; j++) {
                expr.addTerm(costPlantsToWarehouses[i][j], flowPlantsToWarehouses[i][j]);
            }
        }
        // costs from warehouses to markets
        for (int i = 0; i < numWarehouses; i++) {
            for (int j = 0; j < numMarkets; j++) {
                expr.addTerm(costWarehousesToMarkets[i][j], flowWarehousesToMarkets[i][j]);
            }
        }
        // costs for decision variables
        expr.addTerm(1500000, decisionIncreaseDenverPlantCapacity);
        expr.addTerm(2000000, decisionOpenWichitaPlant);
        expr.addTerm(250000, decisionIncreaseWarehouseCapacities[0]); // decision to increase Chicago capacity
        expr.addTerm(200000, decisionIncreaseWarehouseCapacities[1]); // decision to increase Salt Lake City capacity

        cplex.addMinimize(expr);

        // constraints
        // exclusive disjunction; either increase Denver capacity or open Wichita plant
        expr = cplex.linearNumExpr();
        expr.addTerm(decisionIncreaseDenverPlantCapacity, 1);
        expr.addTerm(decisionOpenWichitaPlant, 1);
        cplex.addLe(expr, 1);

        // capacity constraint for the Denver plant
        expr = cplex.linearNumExpr();
        for (int i = 0; i < numWarehouses; i++) {
            expr.addTerm(flowPlantsToWarehouses[0][i], 1);
        }
        expr.addTerm(decisionIncreaseDenverPlantCapacity, -1000); // if 1 will increase capacity by 1000
        cplex.addLe(expr, 1500);

        // capacity constraint for the Wichita plant
        expr = cplex.linearNumExpr();
        for (int i = 0; i < numWarehouses; i++) {
            expr.addTerm(flowPlantsToWarehouses[1][i], 1);
        }
        expr.addTerm(decisionOpenWichitaPlant, -1500); // if 1 will "build" the Wichita plant
        cplex.addLe(expr, 0);

        // capacity constraints for the warehouses
        for (int i = 0; i < numWarehouses; i++) {
            expr = cplex.linearNumExpr();
            for (int j = 0; j < numMarkets; j++) {
                expr.addTerm(flowWarehousesToMarkets[i][j], 1);
            }
            expr.addTerm(decisionIncreaseWarehouseCapacities[i], -1000); // if 1 add 1000 capacity
            cplex.addLe(expr, 1000);
        }

        // at a warehouse, incoming flow from plants must equal outgoing flow to the warehouses
        for (int i = 0; i < numWarehouses; i++) {
            expr = cplex.linearNumExpr();
            for (int j = 0; j < numPlants; j++) {
                expr.addTerm(flowPlantsToWarehouses[j][i], 1); // incoming flow from plants
            }
            for (int j = 0; j < numMarkets; j++) {
                expr.addTerm(flowWarehousesToMarkets[i][j], -1); // outgoing flow to markets
            }
            cplex.addEq(expr, 0);
        }

        // demand constraints for the markets
        for (int i = 0; i < numMarkets; i++) {
            expr = cplex.linearNumExpr();
            for (int j = 0; j < numWarehouses; j++) {
                expr.addTerm(flowWarehousesToMarkets[j][i], 1);
            }
            cplex.addEq(expr, demandMarket[i]);
        }

        cplex.exportModel("capacitated_facility.lp");

        cplex.solve();

        // print out objective value
        System.out.println("objective value: " + cplex.getObjValue());

        // print out decision variables
        System.out.println("----decision variables----");
        System.out.println("increase Denver plant capacity: " + cplex.getValue(decisionIncreaseDenverPlantCapacity));
        System.out.println("open Wichita plant: " + cplex.getValue(decisionOpenWichitaPlant));
        System.out.println("increase Chicago warehouse capacity: " + cplex.getValue(decisionIncreaseWarehouseCapacities[0]));
        System.out.println("increase Salt Lake City warehouse capacity: " + cplex.getValue(decisionIncreaseWarehouseCapacities[1]));

        // print out flows from plants to warehouses
        System.out.println("----flows from plants to warehouses----");
        for (int i = 0; i < numPlants; i++) {
            for (int j = 0; j < numWarehouses; j++) {
                System.out.println(plants[i] + " to " + warehouses[j] + ": " + cplex.getValue(flowPlantsToWarehouses[i][j]));
            }
        }

        // print out flows from warehouses to markets
        System.out.println("----flows from warehouses to markets----");
        for (int i = 0; i < numWarehouses; i++) {
            for (int j = 0; j < numMarkets; j++) {
                System.out.println(warehouses[i] + " to " + markets[j] + ": " + cplex.getValue(flowWarehousesToMarkets[i][j]));
            }
        }
    }

    public static void main(String[] args) throws IloException {
        solveCapacitatedFacility();
    }
}
