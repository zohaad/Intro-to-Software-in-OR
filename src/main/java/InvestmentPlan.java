/*
    Zohaad Fazal
    ID: i6107208
    Date: January 15, 2020
    Exercise A0: Investment Plan 1
    This code is my own work, it was developed without using or copying
    code from other students or other resources.
*/

import ilog.concert.*;
import ilog.cplex.IloCplex;

public class InvestmentPlan {
    IloCplex cplex;

    int numProjects = 6;
    int numYears = 4;

    IloNumVar[] fractionsOfProjects; // what fraction of each project is used
    IloRange[] availableFunds;

    // parameters that both solve methods need
    int[] funds = {60, 70, 35, 20};
    double[][] cashOutlays = {
            {10.5, 14.4, 2.2, 2.4},
            {8.3, 12.6, 9.5, 3.1},
            {10.2, 14.2, 5.6, 4.2},
            {7.2, 10.5, 7.5, 5.0},
            {12.3, 10.1, 8.3, 6.3},
            {9.2, 7.8, 6.9, 5.1}
    };

    IloNumVar[] adjustedSlacks; // slack variables the adjusted investment plan needs

    IloRange additionalConstraint; // additional constraint that project 6's fraction is at least project 2's

    /*
     constructor
     this function initializes the IloCplex object that we need for both parts of the problem
    */
    public InvestmentPlan() throws IloException {
        /*
         * throws IloException is better than doing:
         * try {
         * } catch (IloException e) {
         *   e.printStackTrace();
         * }
         * because exceptions contain: Type, message, trace and cause
         * you're throwing 3/4 things away and continuing with the program
         * if you must catch stuff, do:
         * throw new RuntimeException("Unhandled", e); instead of e.printStackTrace();
         * this preserves all 4 things and stops execution
         * */

        cplex = new IloCplex();
        cplex.setOut(null); // no output when solving the linear program

        // define parameters
        double[] returns = {32.40, 39.80, 37.75, 34.80, 38.20, 27.35};


        // define variable array: numProjects many float variables, set min to 0 and max to 1
        fractionsOfProjects = cplex.numVarArray(numProjects, 0, 1, IloNumVarType.Float);

        // define objective function
        IloLinearNumExpr objective = cplex.linearNumExpr();
        for (int i = 0; i < numProjects; i++) {
            objective.addTerm(fractionsOfProjects[i], returns[i]);
        }
        cplex.addMaximize(objective);

        // create array to keep track of available funds
        availableFunds = new IloRange[numYears];

        // the constraints differ for both problems, the rest is done in the solve methods
    }

    public void solveInvestmentPlan() throws IloException {

        // add the constraints
        for (int j = 0; j < numYears; j++) {
            IloLinearNumExpr expr = cplex.linearNumExpr();
            // for every project add the cash outlay
            for (int i = 0; i < numProjects; i++) {
                expr.addTerm(fractionsOfProjects[i], cashOutlays[i][j]);
            }
            // must be smaller than the funds for that year
            availableFunds[j] = cplex.addLe(expr, funds[j]);
        }

        cplex.exportModel("investment_plan.lp");

        cplex.solve();

        // print the questions:
        // objective value and project mix
        printObjAndProjectMix();
        // slacks
        for (int i = 0; i < numYears; i++) {
            System.out.println("slack" + (i + 1) + ": " + cplex.getSlack(availableFunds[i]));
        }
        // shadow prices
        for (int i = 0; i < numYears; i++) {
            System.out.println("shadow" + (i + 1) + ": " + cplex.getDual(availableFunds[i]));
        }
        // save optimality range
        double[] lowerObj = new double[numProjects];
        double[] upperObj = new double[numProjects];
        cplex.getObjSA(lowerObj, upperObj, fractionsOfProjects);

        // project 6's optimality range
        System.out.println("upper bound of optimality range for project 6: " + upperObj[5]);
    }

    public void solveAdjustedInvestmentPlan() throws IloException {

        // initialize the slack variables the adjusted investment plan needs
        // 100 as an arbitrary upper bound that is large enough (100 > 70)
        adjustedSlacks = cplex.numVarArray(numProjects, 0, 100, IloNumVarType.Float);

        // add the constraints
        for (int j = 0; j < numYears; j++) {
            IloLinearNumExpr expr = cplex.linearNumExpr();
            // for every project add the cash outlay
            for (int i = 0; i < numProjects; i++) {
                expr.addTerm(fractionsOfProjects[i], cashOutlays[i][j]);
            }
            // in the first year we only need to add the slack variable to the left hand side
            expr.addTerm(adjustedSlacks[j], 1);
            // in the other years we also need to subtract the previous year's slack variable
            // this is because we can use the previous year's funds that are left
            if (j > 0) {
                expr.addTerm(adjustedSlacks[j - 1], -1);
            }
            // equality because we manually added the slacks in
            availableFunds[j] = cplex.addEq(expr, funds[j]);
        }

        // additional constraint: if project 2 is partially undertaken, project 6 must be undertaken with an
        // equal or larger fraction: project 6 >= project 2
        // TODO: is there a better way to code this?
        IloLinearNumExpr expr = cplex.linearNumExpr();
        expr.addTerm(fractionsOfProjects[5], 1);
        expr.addTerm(fractionsOfProjects[1], -1);
        additionalConstraint = cplex.addGe(expr, 0);

        cplex.exportModel("adjusted_investment_plan.lp");

        cplex.solve();

        // print the questions:
        // objective value and project mix
        printObjAndProjectMix();
    }

    public void printObjAndProjectMix() throws IloException {
        // objective value
        System.out.println("objective value: " + cplex.getObjValue());
        // optimal project mix
        System.out.println("optimal project mix:");
        for (int i = 0; i < numProjects; i++) {
            System.out.println("x" + (i + 1) + ": " + cplex.getValue(fractionsOfProjects[i]));
        }
    }

    public static void main(String[] args) throws IloException {

        System.out.println("investment plan:");

        InvestmentPlan investmentPlan = new InvestmentPlan();
        investmentPlan.solveInvestmentPlan();

        System.out.println();
        System.out.println("adjusted investment plan:");

        investmentPlan = new InvestmentPlan();
        investmentPlan.solveAdjustedInvestmentPlan();
    }
}
