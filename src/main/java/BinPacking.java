/*
    Zohaad Fazal
    ID: i6107208
    Date: January 17, 2020
    Exercise C0: The Bin Packing Problem
    This code is my own work, it was developed without using or copying
    code from other students or other resources.
*/

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class BinPacking {
    String instanceName;
    int numItems;
    int numBins;
    List<Float> itemSizes;
    IloCplex cplex;
    IloNumVar[] binsUsed;
    IloNumVar[][] itemPlacement;

    public BinPacking(List<String> lines) throws IloException {
        instanceName = lines.get(0);
        numItems = Integer.parseInt(lines.get(1));
        numBins = numItems; // in the worst case we need a bin for each item

        // parameters
        // array with item sizes
        itemSizes = getItemSizesFromLines(lines);

        cplex = new IloCplex();
        cplex.setOut(null);

        // running time must not exceed 1 hour
        cplex.setParam(IloCplex.DoubleParam.TimeLimit, 3600.0);

    }

    public List<Float> getItemSizesFromLines(List<String> lines) {
        return lines.subList(2, numItems + 2)
                .stream()
                .map(Float::parseFloat)
                .collect(Collectors.toList());
    }

    public void solveBinPacking() throws IloException {
        // variables
        binsUsed = cplex.numVarArray(numBins, 0, 1, IloNumVarType.Bool);

        // mapping from items to bins: which item goes in which bin
        // rows: item i, cols: bin j
        itemPlacement = new IloNumVar[numItems][];
        for (int i = 0; i < numItems; i++) {
            itemPlacement[i] = cplex.numVarArray(numBins, 0, 1, IloNumVarType.Bool);
        }

        defineObjectiveAndConstraints();

        cplex.exportModel("bin_packing_" + instanceName + ".lp");

        // print the solving time of cplex
        runAndPrintTimeInMillis(cplex::solve);

        System.out.println(cplex.getObjValue());
    }

    public void solveBinPackingRelaxation() throws IloException {
        // variables
        // relaxation has Float variables
        binsUsed = cplex.numVarArray(numBins, 0, 1, IloNumVarType.Float);

        // mapping from items to bins: which item goes in which bin
        // rows: item i, cols: bin j
        itemPlacement = new IloNumVar[numItems][];
        for (int i = 0; i < numItems; i++) {
            itemPlacement[i] = cplex.numVarArray(numBins, 0, 1, IloNumVarType.Float); // again, Float
        }

        defineObjectiveAndConstraints();

        cplex.exportModel("bin_packing_relaxation_" + instanceName + ".lp");

        // print the solving time of cplex
        runAndPrintTimeInMillis(cplex::solve);

        System.out.println(cplex.getObjValue());
    }

    public void defineObjectiveAndConstraints() throws IloException {
        // objective function
        IloLinearNumExpr expr = cplex.linearNumExpr();
        for (int i = 0; i < numItems; i++) {
            expr.addTerm(binsUsed[i], 1);
        }
        cplex.addMinimize(expr);

        // constraints
        // if a bin is used: capacity <= 1
        // if a bin is not used: capacity <= 0
        for (int j = 0; j < numBins; j++) {
            expr = cplex.linearNumExpr();
            for (int i = 0; i < numItems; i++) {
                expr.addTerm(itemPlacement[i][j], itemSizes.get(i));
            }
            expr.addTerm(binsUsed[j], -1);
            cplex.addLe(expr, 0);
        }

        // one bin per item
        for (int i = 0; i < numItems; i++) {
            expr = cplex.linearNumExpr();
            for (int j = 0; j < numBins; j++) {
                expr.addTerm(itemPlacement[i][j], 1);
            }
            cplex.addEq(expr, 1);
        }
    }

    // prints out the objective value and the item placement matrix
    public void printSolution() throws IloException {
        System.out.println(instanceName + ", bins used: " + cplex.getObjValue());

        System.out.println("item placements:");

        // column (bin) numbering
        System.out.print("   "); // some formatting
        for (int j = 0; j < numBins; j++) {
            System.out.print((j + 1) + " ");
        }
        System.out.println();

        // print out item placement
        for (int i = 0; i < numItems; i++) {
            // row (item) numbering
            String itemNumber = (i + 1) + ":";

            if (i < 9) {
                itemNumber += " "; // need extra space for formatting
            }
            System.out.print(itemNumber);

            // print out item placement values
            for (int j = 0; j < numBins; j++) {
                int itemInBin = (int) cplex.getValue(itemPlacement[i][j]);
                System.out.print(itemInBin + " ");
            }
            System.out.println();
        }
    }

    public static List<Path> getFilePathsFromPathName(String pathName) throws IOException {
        return Files.walk(Paths.get(pathName)) // makes it into a stream
                .filter(Files::isRegularFile)
                .filter(x -> x.getFileName().toString().startsWith("instance"))
                .sorted()
                .collect(Collectors.toList());
    }

    // takes in a method reference or a lambda expression
    public static void runAndPrintTimeInMillis(Timeable t) throws IloException {
        // measure running time
        Instant start = Instant.now();
        t.run();
        Instant finish = Instant.now();

        long duration = Duration.between(start, finish).toMillis();

        System.out.println("duration (ms): " + duration);
    }

    public static void main(String[] args) throws Exception {

        // we want to explain instance1
        BinPacking instance1 = null;

        // get all files from the directory
        List<Path> filePaths = getFilePathsFromPathName("C0-Bin_Packing");

        for (Path filePath : filePaths) {
            // get all lines from the file
            List<String> lines = Files.readAllLines(filePath);

            // construct object using these lines
            BinPacking binPacking = new BinPacking(lines);

            // print out instance name
            System.out.println(binPacking.instanceName + ": ");

            binPacking.solveBinPacking();

            // for C0 (6) we want to explain instance1, so we save it
            if (binPacking.instanceName.equals("instance1")) {
                instance1 = binPacking;
            }

            // relaxation
            System.out.println("relaxation: ");

            // need to recreate the object
            binPacking = new BinPacking(lines);

            binPacking.solveBinPackingRelaxation();
        }

        // QUESTION  C0 (6): explain instance 1 in words
        // prints the objective value and item placement matrix
        instance1.printSolution();

    }
}
