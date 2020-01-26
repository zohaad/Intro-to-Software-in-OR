/*
    Zohaad Fazal
    ID: i6107208
    Date: January 18, 2020
    Exercise D0: Facility Location
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FacilityLocation {
    String instanceName;
    int numRegions;
    int numArcs;
    int numFacilities;
    double[][] distanceMatrix;
    IloCplex cplex;
    IloNumVar[] facilities;
    IloNumVar[][] customerAllocations;

    public FacilityLocation(List<String> lines, int numFacilities) throws IloException {
        instanceName = lines.get(0).split(" ")[0]; // to remove trailing spaces
        numRegions = Integer.parseInt(lines.get(1));
        numArcs = Integer.parseInt(lines.get(2));
        this.numFacilities = numFacilities;

        // create adjacency matrix, with all pairs shortest paths using Floyd-Warshall algorithm
        setDistanceMatrixFromLines(lines);

        // initialize cplex object
        cplex = new IloCplex();
        cplex.setOut(null);

        // define variables
        defineVariables();
        // define objective and constraints
        defineObjectiveAndConstraints();

        cplex.exportModel("facility_location_" + instanceName + "p" + numFacilities + ".lp");
    }

    public void defineVariables() throws IloException {
        facilities = cplex.numVarArray(numRegions, 0, 1, IloNumVarType.Bool);

        customerAllocations = new IloNumVar[numRegions][];
        for (int i = 0; i < numRegions; i++) {
            customerAllocations[i] = cplex.numVarArray(numRegions, 0, 1, IloNumVarType.Bool);
        }
    }

    public void defineObjectiveAndConstraints() throws IloException {
        // objective function
        IloLinearNumExpr expr = cplex.linearNumExpr();
        for (int i = 0; i < numRegions; i++) {
            for (int j = 0; j < numRegions; j++) {
                expr.addTerm(customerAllocations[i][j], distanceMatrix[i][j]);
            }
        }
        cplex.addMinimize(expr);

        // constraints
        // need exactly numFacilities facilities
        expr = cplex.linearNumExpr();
        for (int i = 0; i < numRegions; i++) {
            expr.addTerm(facilities[i], 1);
        }
        cplex.addEq(expr, numFacilities);

        // a customer can only be allocated to region j if region j has a facility
        for (int i = 0; i < numRegions; i++) {
            for (int j = 0; j < numRegions; j++) {
                expr = cplex.linearNumExpr();
                expr.addTerm(customerAllocations[i][j], 1);
                expr.addTerm(facilities[j], -1);
                cplex.addLe(expr, 0);
            }
        }

        // a customer must go to precisely 1 facility
        for (int i = 0; i < numRegions; i++) {
            expr = cplex.linearNumExpr();
            for (int j = 0; j < numRegions; j++) {
                expr.addTerm(customerAllocations[i][j], 1);
            }
            cplex.addEq(expr, 1);
        }
    }

    public void solveFacilityLocation() throws IloException {
        runAndPrintTimeInMillis(cplex::solve); // need to time only the cplex.solve() method
        System.out.println(instanceName + ", p=" + numFacilities + ": " + cplex.getObjValue());
    }

    public void printSolution() throws IloException {
        System.out.println(instanceName + ", p=" + numFacilities + ": " + cplex.getObjValue());

        System.out.println("facilities opened:");

        for (int i = 0; i < numRegions; i++) {
            int facilityPlaced = (int) cplex.getValue(facilities[i]);
            if (facilityPlaced == 1) {
                System.out.print((i + 1) + " "); // we count from 0, so increment
            }
        }

        System.out.println("\ncustomer allocation matrix:");
        // print customer allocations matrix
        for (int i = 0; i < numRegions; i++) {
            for (int j = 0; j < numRegions; j++) {
                int customerAllocation = (int) cplex.getValue(customerAllocations[i][j]);
                System.out.print(customerAllocation + " ");
            }
            System.out.println();
        }
    }

    public void setDistanceMatrixFromLines(List<String> lines) {
        distanceMatrix = new double[numRegions][numRegions];

        // initialize
        for (int i = 0; i < numRegions; i++) {
            for (int j = 0; j < numRegions; j++) {
                if (i == j) {
                    distanceMatrix[i][j] = 0.0; // fill trace with 0
                } else {
                    distanceMatrix[i][j] = Double.MAX_VALUE; // fill with "infinity"
                }
            }
        }

        // add edges
        List<String> edges = lines.subList(3, numArcs + 3);

        for (String edge : edges) {
            String[] edgeInfo = edge.split(" ");
            int i = Integer.parseInt(edgeInfo[0]) - 1; // subtract 1 because counting starts at 0
            int j = Integer.parseInt(edgeInfo[1]) - 1;
            double w = Double.parseDouble(edgeInfo[2]);

            // set weights in adjacency matrix
            distanceMatrix[i][j] = w;
            distanceMatrix[j][i] = w; // undirected graph
        }

        // apply Floyd-Warshall algorithm to compute all pairs shortest paths
        for (int k = 0; k < numRegions; k++) {
            for (int i = 0; i < numRegions; i++) {
                for (int j = 0; j < numRegions; j++) {
                    if (distanceMatrix[i][j] > distanceMatrix[i][k] + distanceMatrix[k][j]) {
                        distanceMatrix[i][j] = distanceMatrix[i][k] + distanceMatrix[k][j];
                    }
                }
            }
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

        List<Path> filePaths = getFilePathsFromPathName("D-FacilityLocation");

        // save instance1 for D0 (6)
        List<FacilityLocation> instance1 = new ArrayList<>();

        for (Path filePath : filePaths) {
            List<String> lines = Files.readAllLines(filePath);

            // loop over number of facilities: 2, 3, and 4
            for (int numFacilities = 2; numFacilities < 5; numFacilities++) {
                FacilityLocation facilityLocation = new FacilityLocation(lines, numFacilities);
                facilityLocation.solveFacilityLocation();

                // save instance 1, for question D0 (6)
                if (facilityLocation.instanceName.equals("instance1")) {
                    instance1.add(facilityLocation);
                }
            }
        }

        System.out.println("\n--- Instance 1 solutions: ---\n");

        for (FacilityLocation facilityLocation : instance1) {
            // prints objective value, which facilities are opened, and the optimal customer allocations matrix
            facilityLocation.printSolution();
            System.out.println();
        }
    }
}